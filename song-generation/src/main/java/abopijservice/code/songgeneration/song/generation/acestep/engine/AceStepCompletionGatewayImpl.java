package abopijservice.code.songgeneration.song.generation.acestep.engine;

import abopijservice.code.songgeneration.song.generation.acestep.AceStepCompletionGateway;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepCompletionResponseReader;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepProperties;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepChatCompletionRequest;
import abopijservice.code.songgeneration.song.generation.acestep.dto.AceStepCompletionResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AceStepCompletionGatewayImpl implements AceStepCompletionGateway {

    private final RestClient restClient;
    private final AceStepProperties properties;
    private final AceStepCompletionResponseReader responseReader;

    public AceStepCompletionGatewayImpl(
            @Qualifier("aceStepRestClient") RestClient restClient,
            AceStepProperties properties,
            AceStepCompletionResponseReader responseReader
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.responseReader = responseReader;
    }

    @Override
    public AceStepCompletionResult createCompletion(AceStepChatCompletionRequest request) {
        properties.requireApiKeyForAceCloud();

        return restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(
                        MediaType.TEXT_EVENT_STREAM,
                        MediaType.APPLICATION_JSON
                )
                .body(request)
                .exchange(
                        (httpRequest, httpResponse) -> responseReader.read(httpResponse)
                );
    }
}
