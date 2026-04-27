package abopijservice.code.songgeneration.song.generation.response;

import java.util.List;

public record SongGenerationResponse(
        String taskId,
        Integer status,
        String downloadUrl,
        String minioKey,
        List<String> minioKeys,
        String metadata
) {
}
