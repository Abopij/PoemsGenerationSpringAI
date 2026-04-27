package abopijservice.code.songgeneration.song.generation.acestep;

import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepChatCompletionRequest;
import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;

public interface AceStepCompletionRequestFactory {

    AceStepChatCompletionRequest create(AceStepGenerationRequest request);
}
