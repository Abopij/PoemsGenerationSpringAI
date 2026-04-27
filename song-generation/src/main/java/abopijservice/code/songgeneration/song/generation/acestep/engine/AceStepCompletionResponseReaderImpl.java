package abopijservice.code.songgeneration.song.generation.acestep.engine;

import abopijservice.code.songgeneration.song.generation.acestep.AceStepCompletionResponseReader;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepCompletionResult;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class AceStepCompletionResponseReaderImpl implements AceStepCompletionResponseReader {

    private static final ObjectMapper JSON = JsonMapper.builder().build();

    @Override
    public AceStepCompletionResult read(ClientHttpResponse response) throws IOException {
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
                if (StringUtils.hasText(payload)) {
                    accumulator.accept(JSON.readTree(payload));
                }
            }
        }

        if (sawStreamingEvent) {
            return accumulator.toResult();
        }
        if (StringUtils.hasText(rawResponse)) {
            return readJsonResponse(rawResponse.toString());
        }
        throw new IllegalStateException("ACE Music returned an empty response");
    }

    private static AceStepCompletionResult readJsonResponse(String rawResponse) throws IOException {
        JsonNode response = JSON.readTree(rawResponse);
        assertNoOpenAiError(response);
        return new AceStepCompletionResult(
                text(response, "id"),
                extractAudioUrls(response),
                text(response.path("choices").path(0).path("message"), "content")
        );
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

    private static List<String> extractAudioUrls(JsonNode response) {
        List<String> audioUrls = new ArrayList<>();
        JsonNode choices = response == null ? MissingNode.getInstance() : response.path("choices");
        if (!choices.isArray()) {
            return audioUrls;
        }

        for (JsonNode choice : choices) {
            JsonNode audio = choice.path("message").path("audio");
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
        return audioUrls;
    }

    private static void assertNoOpenAiError(JsonNode response) {
        JsonNode error = response == null ? MissingNode.getInstance() : response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String message = error.isString() ? error.asString() : text(error, "message");
            throw new IllegalStateException("ACE Music API error: " + valueOrDefault(message, error.toString()));
        }
    }

    private static String text(JsonNode node, String... path) {
        JsonNode current = node == null ? MissingNode.getInstance() : node;
        for (String field : path) {
            current = current.path(field);
        }
        return current.isMissingNode() || current.isNull() ? null : current.asString();
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

        AceStepCompletionResult toResult() {
            return new AceStepCompletionResult(taskId, List.copyOf(audioUrls), metadata.toString());
        }
    }
}
