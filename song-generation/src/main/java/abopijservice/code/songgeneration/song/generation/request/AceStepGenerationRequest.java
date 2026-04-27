package abopijservice.code.songgeneration.song.generation.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AceStepGenerationRequest(
        @JsonAlias("prompt") String caption,
        String lyrics,
        Double duration,
        Integer bpm,
        @JsonAlias("format") String audioFormat,
        String model,
        Double temperature,
        @JsonAlias("top_p") Double topP,
        Object seed,
        @JsonAlias("sample_mode") Boolean sampleMode,
        Boolean thinking,
        @JsonAlias("use_format") Boolean useFormat,
        @JsonAlias("use_cot_caption") Boolean useCotCaption,
        @JsonAlias("use_cot_language") Boolean useCotLanguage,
        @JsonAlias("guidance_scale") Double guidanceScale,
        @JsonAlias("batch_size") Integer batchSize,
        @JsonAlias("task_type") String taskType,
        @JsonAlias("repainting_start") Double repaintingStart,
        @JsonAlias("repainting_end") Double repaintingEnd,
        @JsonAlias("audio_cover_strength") Double audioCoverStrength,
        @JsonAlias("vocal_language") String vocalLanguage,
        Boolean instrumental,
        @JsonAlias("key_scale") String keyScale,
        @JsonAlias("time_signature") String timeSignature,
        @JsonAlias("input_audio_base64") String inputAudioBase64,
        @JsonAlias("input_audio_format") String inputAudioFormat,
        @JsonAlias("reference_audio_base64") String referenceAudioBase64,
        @JsonAlias("reference_audio_format") String referenceAudioFormat
) {
    public static AceStepGenerationRequest simple(String prompt, String lyrics) {
        return new AceStepGenerationRequest(
                prompt,
                lyrics,
                null,
                null,
                "mp3",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
