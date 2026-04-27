package abopijservice.code.songgeneration.song.generation.acestep.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AceStepInputAudioContentPart(
        String type,
        @JsonProperty("input_audio") AceStepInputAudio inputAudio
) implements AceStepMessageContentPart {

    public AceStepInputAudioContentPart(AceStepInputAudio inputAudio) {
        this("input_audio", inputAudio);
    }
}
