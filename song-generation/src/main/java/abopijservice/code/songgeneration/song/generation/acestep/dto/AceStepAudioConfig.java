package abopijservice.code.songgeneration.song.generation.acestep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AceStepAudioConfig(
        Double duration,
        Integer bpm,
        @JsonProperty("vocal_language") String vocalLanguage,
        Boolean instrumental,
        String format,
        @JsonProperty("key_scale") String keyScale,
        @JsonProperty("time_signature") String timeSignature
) {
}
