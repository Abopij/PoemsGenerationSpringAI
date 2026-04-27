package abopijservice.code.songgeneration.grpc;

import abopijservice.code.grpc.songpoem.GenerationSongGrpcControllerGrpc;
import abopijservice.code.grpc.songpoem.SongGenerationRequest;
import abopijservice.code.grpc.songpoem.SongGenerationResponse;
import abopijservice.code.songgeneration.song.generation.SongGenerationService;
import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GenerationSongGrpcControllerImpl
        extends GenerationSongGrpcControllerGrpc.GenerationSongGrpcControllerImplBase {

    private final SongGenerationService songGenerationService;

    public GenerationSongGrpcControllerImpl(SongGenerationService songGenerationService) {
        this.songGenerationService = songGenerationService;
    }

    @Override
    public void generate(SongGenerationRequest request, StreamObserver<SongGenerationResponse> responseObserver) {
        try {
            var response = songGenerationService.generateSong(
                    AceStepGenerationRequest.simple(request.getStyles(), request.getPoem())
            );

            responseObserver.onNext(SongGenerationResponse.newBuilder()
                    .setLink(valueOrEmpty(response.minioKey()))
                    .setMinioKey(valueOrEmpty(response.minioKey()))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
