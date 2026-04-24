package abopijservice.code.songgeneration.song.generation;

import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import abopijservice.code.songgeneration.song.generation.response.SongGenerationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AceStepClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;
    private final String baseUrl;
    private final long pollIntervalMs;
    private final long timeoutSeconds;

    public AceStepClient(
            @Value("${ace-step.base-url:http://localhost:8001}") String baseUrl,
            @Value("${ace-step.poll-interval-ms:2000}") long pollIntervalMs,
            @Value("${ace-step.timeout-seconds:300}") long timeoutSeconds
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.pollIntervalMs = pollIntervalMs;
        this.timeoutSeconds = timeoutSeconds;
        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .build();
    }

    public SongGenerationResponse generateSong(AceStepGenerationRequest request) {
        String taskId = submit(request);
        JsonNode taskResult = pollUntilDone(taskId);
        int status = taskResult.path("status").asInt(-1);
        if (status != 1) {
            throw new IllegalStateException("ACE-Step generation failed for task " + taskId + ". Raw payload: " + taskResult);
        }

        JsonNode firstAudio = firstGeneratedAudio(taskResult.path("result").asText(null));
        String downloadUrl = resolveDownloadUrl(text(firstAudio, "file"));
        if (!StringUtils.hasText(downloadUrl)) {
            throw new IllegalStateException("ACE-Step returned no file URL for task " + taskId);
        }

        return new SongGenerationResponse(taskId, status, downloadUrl);
    }

    private String submit(AceStepGenerationRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        putIfHasText(body, "prompt", request.prompt());
        putIfHasText(body, "lyrics", request.lyrics());
        putIfNotNull(body, "duration", request.duration());
        putIfNotNull(body, "bpm", request.bpm());
        body.put("task_type", "text2music");
        body.put("thinking", true);

        JsonNode response = restClient.post()
                .uri("/release_task")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String taskId = text(response, "data", "task_id");
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalStateException("ACE-Step did not return task_id");
        }

        return taskId;
    }

    private JsonNode pollUntilDone(String taskId) {
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        while (Instant.now().isBefore(deadline)) {
            JsonNode response = restClient.post()
                    .uri("/query_result")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("task_id_list", List.of(taskId)))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode taskResult = findTask(response == null ? MissingNode.getInstance() : response.path("data"), taskId);
            int status = taskResult.path("status").asInt(-1);

            if (status == 1 || status == 2) {
                return taskResult;
            }

            sleep(Duration.ofMillis(pollIntervalMs));
        }

        throw new IllegalStateException("Timed out while waiting for ACE-Step task " + taskId);
    }

    private JsonNode firstGeneratedAudio(String resultJson) {
        if (!StringUtils.hasText(resultJson)) {
            return MissingNode.getInstance();
        }

        try {
            JsonNode result = JSON.readTree(resultJson);
            if (result.isArray()) {
                return result.isEmpty() ? MissingNode.getInstance() : result.get(0);
            }
            return result;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse ACE-Step result payload", exception);
        }
    }

    private String resolveDownloadUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || isAbsoluteUrl(fileUrl)) {
            return fileUrl;
        }

        return fileUrl
                .startsWith("/") ?
                baseUrl + fileUrl
                :
                baseUrl + "/" + fileUrl;
    }

    private static void putIfHasText(
            Map<String, Object> body,
            String key,
            String value
    ) {
        if (StringUtils.hasText(value)) {
            body.put(key, value);
        }
    }

    private static void putIfNotNull(
            Map<String, Object> body,
            String key,
            Object value
    ) {
        if (value != null) {
            body.put(key, value);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ACE-Step result", exception);
        }
    }

    private static JsonNode findTask(JsonNode tasks, String taskId) {
        if (!tasks.isArray()) {
            return MissingNode.getInstance();
        }
        for (JsonNode task : tasks) {
            if (taskId.equals(text(task, "task_id"))) {
                return task;
            }
        }
        return MissingNode.getInstance();
    }

    private static String text(
            JsonNode node,
            String... path
    ) {
        JsonNode current = node == null ? MissingNode.getInstance() : node;
        for (String field : path) {
            current = current.path(field);
        }
        return current.isMissingNode() || current.isNull() ? null : current.asText();
    }

    private static String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static boolean isAbsoluteUrl(String value) {
        return StringUtils.hasText(value) && (value.startsWith("http://") || value.startsWith("https://"));
    }
}
