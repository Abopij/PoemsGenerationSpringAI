package abopijservice.code.songpoemautogeneration;

import com.fasterxml.jackson.annotation.JsonAlias;

public record SongByPoemAutoGenerationRequest(
        @JsonAlias("original_poem") String originalPoem,
        String styles
) {
}
