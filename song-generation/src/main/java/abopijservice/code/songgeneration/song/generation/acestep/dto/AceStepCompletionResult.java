package abopijservice.code.songgeneration.song.generation.acestep.dto;

import java.util.List;

public record AceStepCompletionResult(
        String taskId,
        List<String> audioUrls,
        String metadata
) {
}
