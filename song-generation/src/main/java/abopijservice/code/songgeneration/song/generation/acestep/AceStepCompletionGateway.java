package abopijservice.code.songgeneration.song.generation.acestep;

import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepChatCompletionRequest;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepCompletionResult;

public interface AceStepCompletionGateway {

    AceStepCompletionResult createCompletion(AceStepChatCompletionRequest request);
}
