package abopijservice.code.songgeneration.song.generation.acestep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AceStepChatCompletionRequest(
        String model,
        List<AceStepMessage> messages,
        boolean stream,
        String lyrics,
        Double temperature,
        @JsonProperty("top_p") Double topP,
        Object seed,
        @JsonProperty("sample_mode") Boolean sampleMode,
        Boolean thinking,
        @JsonProperty("use_format") Boolean useFormat,
        @JsonProperty("use_cot_caption") Boolean useCotCaption,
        @JsonProperty("use_cot_language") Boolean useCotLanguage,
        @JsonProperty("guidance_scale") Double guidanceScale,
        @JsonProperty("batch_size") Integer batchSize,
        @JsonProperty("task_type") String taskType,
        @JsonProperty("repainting_start") Double repaintingStart,
        @JsonProperty("repainting_end") Double repaintingEnd,
        @JsonProperty("audio_cover_strength") Double audioCoverStrength,
        @JsonProperty("audio_config") AceStepAudioConfig audioConfig
) {
}
