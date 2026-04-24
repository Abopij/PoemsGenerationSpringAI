package abopijservice.code.songgeneration.song.generation.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AceStepGenerationRequest(
        @JsonAlias("caption") String prompt,
        String lyrics,
        Integer duration,
        Integer bpm
) {}
