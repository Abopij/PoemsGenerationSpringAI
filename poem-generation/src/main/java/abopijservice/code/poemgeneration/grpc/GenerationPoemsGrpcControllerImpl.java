package abopijservice.code.poemgeneration.grpc;

import abopijservice.code.grpc.songpoem.GenerationPoemsGrpcControllerGrpc;
import abopijservice.code.grpc.songpoem.PoemGenerationRequest;
import abopijservice.code.grpc.songpoem.PoemGenerationResponse;
import abopijservice.code.poemgeneration.songpoem.SongPoemPreparationRequest;
import abopijservice.code.poemgeneration.songpoem.SongPoemPreparationResponse;
import abopijservice.code.poemgeneration.songpoem.SongPoemPreparationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.util.StringUtils;

import java.util.UUID;

@GrpcService
public class GenerationPoemsGrpcControllerImpl
        extends GenerationPoemsGrpcControllerGrpc.GenerationPoemsGrpcControllerImplBase {

    private final SongPoemPreparationService songPoemPreparationService;

    public GenerationPoemsGrpcControllerImpl(SongPoemPreparationService songPoemPreparationService) {
        this.songPoemPreparationService = songPoemPreparationService;
    }

    @Override
    public void generate(PoemGenerationRequest request, StreamObserver<PoemGenerationResponse> responseObserver) {
        try {
            SongPoemPreparationResponse response = songPoemPreparationService.prepare(
                    new SongPoemPreparationRequest(
                            parseUuid(request.getChatId()),
                            request.getPrompt(),
                            request.getWords()
                    )
            );

            responseObserver.onNext(
                    PoemGenerationResponse.newBuilder()
                    .setChatId(
                            response
                                    .chatId()
                                    .toString()
                    )
                    .setTitle(
                            valueOrEmpty(
                                    response.title()
                            )
                    )
                    .setPoem(
                            valueOrEmpty(
                                    response.poem()
                            )
                    )
                    .setStyles(
                            valueOrEmpty(
                                    response.styles()
                            )
                    )
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private static UUID parseUuid(String value) {
        return StringUtils.hasText(value) ? UUID.fromString(value) : null;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
