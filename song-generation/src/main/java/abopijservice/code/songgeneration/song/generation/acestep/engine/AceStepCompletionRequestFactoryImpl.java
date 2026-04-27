package abopijservice.code.songgeneration.song.generation.acestep.engine;

import abopijservice.code.songgeneration.song.generation.acestep.AceStepCompletionRequestFactory;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepProperties;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepAudioConfig;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepChatCompletionRequest;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepInputAudio;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepInputAudioContentPart;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepMessage;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepMessageContentPart;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepTextContentPart;
import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AceStepCompletionRequestFactoryImpl implements AceStepCompletionRequestFactory {

    private final AceStepProperties properties;

    @Override
    public AceStepChatCompletionRequest create(AceStepGenerationRequest request) {
        AceStepProperties.Defaults defaults = properties.getDefaults();
        String audioFormat = valueOrDefault(request.audioFormat(), defaults.getAudioFormat());

        return new AceStepChatCompletionRequest(
                valueOrDefault(
                        request.model(),
                        properties.getModel()
                ),
                List.of(
                        new AceStepMessage(
                                "user",
                                messageContent(request, audioFormat)
                        )
                ),
                true,
                lyricsField(request),
                valueOrDefault(
                        request.temperature(),
                        defaults.getTemperature()
                ),
                valueOrDefault(
                        request.topP(),
                        defaults.getTopP()
                ),
                request.seed(),
                valueOrDefault(
                        request.sampleMode(),
                        defaults.isSampleMode()
                ),
                valueOrDefault(
                        request.thinking(),
                        defaults.isThinking()
                ),
                valueOrDefault(
                        request.useFormat(),
                        defaults.isUseFormat()
                ),
                valueOrDefault(
                        request.useCotCaption(),
                        defaults.isUseCotCaption()
                ),
                valueOrDefault(
                        request.useCotLanguage(),
                        defaults.isUseCotLanguage()
                ),
                valueOrDefault(
                        request.guidanceScale(),
                        defaults.getGuidanceScale()
                ),
                valueOrDefault(
                        request.batchSize(),
                        defaults.getBatchSize()
                ),
                valueOrDefault(
                        request.taskType(),
                        defaults.getTaskType()
                ),
                request.repaintingStart(),
                request.repaintingEnd(),
                request.audioCoverStrength(),
                audioConfig(request, audioFormat, defaults)
        );
    }

    private static AceStepAudioConfig audioConfig(
            AceStepGenerationRequest request,
            String audioFormat,
            AceStepProperties.Defaults defaults
    ) {
        return new AceStepAudioConfig(
                request.duration(),
                request.bpm(),
                valueOrDefault(
                        request.vocalLanguage(),
                        defaults.getVocalLanguage()
                ),
                request.instrumental(),
                audioFormat,
                request.keyScale(),
                request.timeSignature()
        );
    }

    private static Object messageContent(AceStepGenerationRequest request, String audioFormat) {
        String text = resolveTextContent(request);
        if (!StringUtils.hasText(request.inputAudioBase64())
                && !StringUtils.hasText(request.referenceAudioBase64())) {
            return text;
        }

        List<AceStepMessageContentPart> content = new ArrayList<>();
        content.add(new AceStepTextContentPart(text));
        addAudioPart(content, request.inputAudioBase64(), request.inputAudioFormat(), audioFormat);
        addAudioPart(content, request.referenceAudioBase64(), request.referenceAudioFormat(), audioFormat);
        return content;
    }

    private static String resolveTextContent(AceStepGenerationRequest request) {
        if (StringUtils.hasText(request.caption())) {
            return request.caption();
        }
        if (StringUtils.hasText(request.lyrics())) {
            return request.lyrics();
        }
        throw new IllegalArgumentException("ACE Music request requires prompt or lyrics");
    }

    private static String lyricsField(AceStepGenerationRequest request) {
        return StringUtils.hasText(request.caption()) ? request.lyrics() : null;
    }

    private static void addAudioPart(
            List<AceStepMessageContentPart> content,
            String base64Audio,
            String format,
            String defaultFormat
    ) {
        if (!StringUtils.hasText(base64Audio)) {
            return;
        }
        content.add(new AceStepInputAudioContentPart(new AceStepInputAudio(
                stripDataUrlPrefix(base64Audio),
                valueOrDefault(format, defaultFormat)
        )));
    }

    private static String stripDataUrlPrefix(String value) {
        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(comma + 1) : value;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static <T> T valueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
