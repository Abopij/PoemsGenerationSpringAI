package abopijservice.code.songgeneration.song.generation;

import abopijservice.code.songgeneration.minio.MinIOService;
import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import abopijservice.code.songgeneration.song.generation.response.SongGenerationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AceStepClient {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String defaultAudioFormat;
    private final String defaultVocalLanguage;
    private final String defaultTaskType;
    private final double defaultTemperature;
    private final double defaultTopP;
    private final boolean defaultSampleMode;
    private final boolean defaultThinking;
    private final boolean defaultUseFormat;
    private final boolean defaultUseCotCaption;
    private final boolean defaultUseCotLanguage;
    private final double defaultGuidanceScale;
    private final int defaultBatchSize;
    private final MinIOService minIOService;

    public AceStepClient(
            @Value("${ace-step.base-url}") String baseUrl,
            @Value("${ace-step.api-key}") String apiKey,
            @Value("${ace-step.model}") String model,
            @Value("${ace-step.timeout-seconds}") long timeoutSeconds,
            @Value("${ace-step.defaults.audio-format}") String defaultAudioFormat,
            @Value("${ace-step.defaults.vocal-language}") String defaultVocalLanguage,
            @Value("${ace-step.defaults.task-type}") String defaultTaskType,
            @Value("${ace-step.defaults.temperature}") double defaultTemperature,
            @Value("${ace-step.defaults.top-p}") double defaultTopP,
            @Value("${ace-step.defaults.sample-mode}") boolean defaultSampleMode,
            @Value("${ace-step.defaults.thinking}") boolean defaultThinking,
            @Value("${ace-step.defaults.use-format}") boolean defaultUseFormat,
            @Value("${ace-step.defaults.use-cot-caption}") boolean defaultUseCotCaption,
            @Value("${ace-step.defaults.use-cot-language}") boolean defaultUseCotLanguage,
            @Value("${ace-step.defaults.guidance-scale}") double defaultGuidanceScale,
            @Value("${ace-step.defaults.batch-size}") int defaultBatchSize,
            MinIOService minIOService
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.defaultAudioFormat = defaultAudioFormat;
        this.defaultVocalLanguage = defaultVocalLanguage;
        this.defaultTaskType = defaultTaskType;
        this.defaultTemperature = defaultTemperature;
        this.defaultTopP = defaultTopP;
        this.defaultSampleMode = defaultSampleMode;
        this.defaultThinking = defaultThinking;
        this.defaultUseFormat = defaultUseFormat;
        this.defaultUseCotCaption = defaultUseCotCaption;
        this.defaultUseCotLanguage = defaultUseCotLanguage;
        this.defaultGuidanceScale = defaultGuidanceScale;
        this.defaultBatchSize = defaultBatchSize;
        this.minIOService = minIOService;

        ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(
                HttpClient.create().responseTimeout(Duration.ofSeconds(timeoutSeconds))
        );

        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(apiKey)) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                    byte[] body = res.getBody().readAllBytes();
                    String raw = new String(body, StandardCharsets.UTF_8);
                    throw new IllegalStateException(
                            "ACE-Step API error [HTTP " + res.getStatusCode() + "]: " + raw
                    );
                })
                .build();
    }

    public SongGenerationResponse generateSong(AceStepGenerationRequest request) {
        CompletionResult result = createCompletion(request);
        String taskId = result.taskId();
        if (!StringUtils.hasText(taskId)) {
            taskId = UUID.randomUUID().toString();
        }

        List<String> audioUrls = result.audioUrls();
        if (audioUrls.isEmpty()) {
            throw new IllegalStateException("ACE Music returned no audio. Metadata: " + result.metadata());
        }

        List<String> minioKeys = new ArrayList<>();
        for (int i = 0; i < audioUrls.size(); i++) {
            minioKeys.add(storeDataUrl(taskId, audioUrls.get(i), i + 1, resolveAudioFormat(request)));
        }

        return new SongGenerationResponse(taskId, 1, null, minioKeys.getFirst(), minioKeys, result.metadata());
    }

    private CompletionResult createCompletion(AceStepGenerationRequest request) {
        assertApiKeyConfigured();

        return restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .body(completionBody(request))
                .exchange((httpRequest, httpResponse) -> readCompletionResponse(httpResponse));
    }

    private Map<String, Object> completionBody(AceStepGenerationRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", valueOrDefault(request.model(), model));
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", messageContent(request)
        )));
        body.put("stream", true);
        if (StringUtils.hasText(request.caption())) {
            putIfHasText(body, "lyrics", request.lyrics());
        }
        body.put("temperature", request.temperature() != null ? request.temperature() : defaultTemperature);
        body.put("top_p", request.topP() != null ? request.topP() : defaultTopP);
        putIfNotNull(body, "seed", request.seed());
        body.put("sample_mode", request.sampleMode() != null ? request.sampleMode() : defaultSampleMode);
        body.put("thinking", request.thinking() != null ? request.thinking() : defaultThinking);
        body.put("use_format", request.useFormat() != null ? request.useFormat() : defaultUseFormat);
        body.put("use_cot_caption", request.useCotCaption() != null ? request.useCotCaption() : defaultUseCotCaption);
        body.put("use_cot_language", request.useCotLanguage() != null ? request.useCotLanguage() : defaultUseCotLanguage);
        body.put("guidance_scale", request.guidanceScale() != null ? request.guidanceScale() : defaultGuidanceScale);
        body.put("batch_size", request.batchSize() != null ? request.batchSize() : defaultBatchSize);
        body.put("task_type", valueOrDefault(request.taskType(), defaultTaskType));
        putIfNotNull(body, "repainting_start", request.repaintingStart());
        putIfNotNull(body, "repainting_end", request.repaintingEnd());
        putIfNotNull(body, "audio_cover_strength", request.audioCoverStrength());
        body.put("audio_config", audioConfig(request));
        return body;
    }

    private CompletionResult readCompletionResponse(ClientHttpResponse response) throws IOException {
        assertSuccessStatus(response);

        StreamingCompletionAccumulator accumulator = new StreamingCompletionAccumulator();
        StringBuilder rawResponse = new StringBuilder();
        boolean sawStreamingEvent = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    if (StringUtils.hasText(line)) {
                        rawResponse.append(line);
                    }
                    continue;
                }

                sawStreamingEvent = true;
                String payload = line.substring("data: ".length()).trim();
                if ("[DONE]".equals(payload)) {
                    break;
                }
                if (!StringUtils.hasText(payload)) {
                    continue;
                }

                accumulator.accept(JSON.readTree(payload));
            }
        }

        if (sawStreamingEvent) {
            return accumulator.toResult();
        }
        if (StringUtils.hasText(rawResponse)) {
            JsonNode responseJson = JSON.readTree(rawResponse.toString());
            assertNoOpenAiError(responseJson);
            return new CompletionResult(
                    text(responseJson, "id"),
                    extractAudioUrls(responseJson),
                    text(responseJson.path("choices").path(0).path("message"), "content")
            );
        }
        throw new IllegalStateException("ACE Music returned an empty response");
    }

    private static void assertSuccessStatus(ClientHttpResponse response) throws IOException {
        if (!response.getStatusCode().isError()) {
            return;
        }

        String raw = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        if (response.getStatusCode().value() == 504) {
            throw new IllegalStateException(
                    "ACE-Step generation timed out on the server side [HTTP 504]. "
                            + "Streaming is enabled, so retry the request or reduce duration/batch_size. Response: "
                            + raw
            );
        }
        throw new IllegalStateException(
                "ACE-Step API error [HTTP " + response.getStatusCode() + "]: " + raw
        );
    }

    private void assertApiKeyConfigured() {
        if (StringUtils.hasText(apiKey) || !baseUrl.contains("api.acemusic.ai")) {
            return;
        }
        throw new IllegalStateException(
                "ACE Music API key is not configured. Set ACE_MUSIC_API_KEY or ace-step.api-key before calling "
                        + baseUrl + "/v1/chat/completions"
        );
    }

    private Map<String, Object> audioConfig(AceStepGenerationRequest request) {
        Map<String, Object> audioConfig = new LinkedHashMap<>();
        putIfNotNull(audioConfig, "duration", request.duration());
        putIfNotNull(audioConfig, "bpm", request.bpm());
        audioConfig.put("vocal_language", valueOrDefault(request.vocalLanguage(), defaultVocalLanguage));
        putIfNotNull(audioConfig, "instrumental", request.instrumental());
        audioConfig.put("format", resolveAudioFormat(request));
        putIfHasText(audioConfig, "key_scale", request.keyScale());
        putIfHasText(audioConfig, "time_signature", request.timeSignature());
        return audioConfig;
    }

    private Object messageContent(AceStepGenerationRequest request) {
        String content = resolveTextContent(request);
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("ACE Music request requires caption/prompt or lyrics");
        }

        if (!StringUtils.hasText(request.inputAudioBase64()) && !StringUtils.hasText(request.referenceAudioBase64())) {
            return content;
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("type", "text", "text", content));
        addAudioPart(parts, request.inputAudioBase64(), request.inputAudioFormat());
        addAudioPart(parts, request.referenceAudioBase64(), request.referenceAudioFormat());
        return parts;
    }

    private String resolveTextContent(AceStepGenerationRequest request) {
        if (StringUtils.hasText(request.caption())) {
            return request.caption();
        }
        return request.lyrics();
    }

    private void addAudioPart(List<Map<String, Object>> parts, String base64Audio, String format) {
        if (!StringUtils.hasText(base64Audio)) {
            return;
        }
        parts.add(Map.of(
                "type", "input_audio",
                "input_audio", Map.of(
                        "data", stripDataUrlPrefix(base64Audio),
                        "format", valueOrDefault(format, defaultAudioFormat)
                )
        ));
    }

    private String storeDataUrl(String taskId, String dataUrl, int index, String fallbackFormat) {
        DecodedAudio decodedAudio = decodeDataUrl(dataUrl, fallbackFormat);
        String objectName = "songs/" + safeObjectName(taskId) + "-" + index + "-" + UUID.randomUUID()
                + decodedAudio.extension();

        if (decodedAudio.bytes().length == 0) {
            throw new IllegalStateException("Downloaded empty audio file for task " + taskId);
        }

        try (InputStream stream = new ByteArrayInputStream(decodedAudio.bytes())) {
            minIOService.uploadStream(stream, objectName, decodedAudio.bytes().length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload audio to MinIO for task " + taskId, e);
        }

        return objectName;
    }

    private static DecodedAudio decodeDataUrl(String dataUrl, String fallbackFormat) {
        if (!StringUtils.hasText(dataUrl)) {
            throw new IllegalStateException("ACE Music returned an empty audio data URL");
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalStateException("ACE Music audio response is not a data URL");
        }

        String header = dataUrl.substring(0, comma);
        String payload = dataUrl.substring(comma + 1);
        byte[] bytes = Base64.getDecoder().decode(payload);
        return new DecodedAudio(bytes, resolveExtension(header, fallbackFormat));
    }

    private static List<String> extractAudioUrls(JsonNode response) {
        List<String> audioUrls = new ArrayList<>();
        JsonNode choices = response == null ? MissingNode.getInstance() : response.path("choices");
        if (choices.isArray()) {
            for (JsonNode choice : choices) {
                JsonNode audio = choice.path("message").path("audio");
                if (audio.isArray()) {
                    for (JsonNode item : audio) {
                        String url = text(item, "audio_url", "url");
                        if (StringUtils.hasText(url)) {
                            audioUrls.add(url);
                        }
                    }
                }
            }
        }
        return audioUrls;
    }

    private static void putIfHasText(Map<String, Object> body, String key, String value) {
        if (StringUtils.hasText(value)) body.put(key, value);
    }

    private static void putIfNotNull(Map<String, Object> body, String key, Object value) {
        if (value != null) body.put(key, value);
    }

    private static String text(JsonNode node, String... path) {
        JsonNode cur = node == null ? MissingNode.getInstance() : node;
        for (String f : path) cur = cur.path(f);
        return cur.isMissingNode() || cur.isNull() ? null : cur.asString();
    }

    private static void assertNoOpenAiError(JsonNode response) {
        JsonNode error = response == null ? MissingNode.getInstance() : response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String message = error.isString() ? error.asString() : text(error, "message");
            throw new IllegalStateException("ACE Music API error: " + valueOrDefault(message, error.toString()));
        }
    }

    private static String stripDataUrlPrefix(String value) {
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(comma + 1) : value;
    }

    private String resolveAudioFormat(AceStepGenerationRequest request) {
        return valueOrDefault(request.audioFormat(), defaultAudioFormat);
    }

    private static String resolveExtension(String dataUrlHeader, String fallbackFormat) {
        if (StringUtils.hasText(dataUrlHeader)) {
            if (dataUrlHeader.contains("audio/wav")) return ".wav";
            if (dataUrlHeader.contains("audio/flac")) return ".flac";
            if (dataUrlHeader.contains("audio/mpeg") || dataUrlHeader.contains("audio/mp3")) return ".mp3";
        }
        String format = StringUtils.hasText(fallbackFormat) ? fallbackFormat.toLowerCase() : "mp3";
        return switch (format) {
            case "wav" -> ".wav";
            case "flac" -> ".flac";
            default -> ".mp3";
        };
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) return value;
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String safeObjectName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static final class StreamingCompletionAccumulator {
        private String taskId;
        private final StringBuilder metadata = new StringBuilder();
        private final List<String> audioUrls = new ArrayList<>();

        void accept(JsonNode chunk) {
            assertNoOpenAiError(chunk);
            if (!StringUtils.hasText(taskId)) {
                taskId = text(chunk, "id");
            }

            JsonNode choices = chunk.path("choices");
            if (!choices.isArray()) {
                return;
            }

            for (JsonNode choice : choices) {
                JsonNode delta = choice.path("delta");
                String content = text(delta, "content");
                if (StringUtils.hasText(content) && !".".equals(content)) {
                    metadata.append(content);
                }

                JsonNode audio = delta.path("audio");
                if (!audio.isArray()) {
                    continue;
                }
                for (JsonNode item : audio) {
                    String url = text(item, "audio_url", "url");
                    if (StringUtils.hasText(url)) {
                        audioUrls.add(url);
                    }
                }
            }
        }

        CompletionResult toResult() {
            return new CompletionResult(taskId, List.copyOf(audioUrls), metadata.toString());
        }
    }

    private record CompletionResult(String taskId, List<String> audioUrls, String metadata) {
    }

    private record DecodedAudio(byte[] bytes, String extension) {
    }
}
