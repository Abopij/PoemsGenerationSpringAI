package abopijservice.code.songgeneration.song.generation.acestep;

import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepCompletionResult;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public interface AceStepCompletionResponseReader {

    AceStepCompletionResult read(ClientHttpResponse response) throws IOException;
}
