package abopijservice.code.songgeneration.song.generation.response;

public record SongGenerationResponse(
        String taskId,
        Integer status,
        String downloadUrl
) {
}
