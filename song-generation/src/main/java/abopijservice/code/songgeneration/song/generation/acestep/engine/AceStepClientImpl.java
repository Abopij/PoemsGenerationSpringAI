package abopijservice.code.songgeneration.song.generation.acestep.engine;

import abopijservice.code.songgeneration.song.generation.acestep.AceStepAudioStorage;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepClient;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepCompletionGateway;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepCompletionRequestFactory;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepChatCompletionRequest;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepCompletionResult;
import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import abopijservice.code.songgeneration.song.generation.response.SongGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AceStepClientImpl implements AceStepClient {

    private final AceStepCompletionRequestFactory requestFactory;
    private final AceStepCompletionGateway completionGateway;
    private final AceStepAudioStorage audioStorage;

    @Override
    public SongGenerationResponse generateSong(AceStepGenerationRequest request) {
        AceStepChatCompletionRequest completionRequest = requestFactory.create(request);
        AceStepCompletionResult completionResult = completionGateway.createCompletion(completionRequest);

        String taskId = resolveTaskId(completionResult);
        List<String> audioUrls = completionResult.audioUrls();
        if (audioUrls == null || audioUrls.isEmpty()) {
            throw new IllegalStateException("ACE Music returned no audio. Metadata: " + completionResult.metadata());
        }

        List<String> minioKeys = audioStorage.store(
                taskId,
                audioUrls,
                completionRequest.audioConfig().format()
        );

        return new SongGenerationResponse(
                taskId,
                1,
                null,
                minioKeys.getFirst(),
                minioKeys,
                completionResult.metadata()
        );
    }

    private static String resolveTaskId(AceStepCompletionResult completionResult) {
        if (StringUtils.hasText(completionResult.taskId())) {
            return completionResult.taskId();
        }
        return UUID.randomUUID().toString();
    }
}
