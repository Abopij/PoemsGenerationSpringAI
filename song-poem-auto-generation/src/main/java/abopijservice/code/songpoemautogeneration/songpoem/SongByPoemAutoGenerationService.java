package abopijservice.code.songpoemautogeneration.songpoem;

import abopijservice.code.grpc.songpoem.GenerationPoemsGrpcControllerGrpc;
import abopijservice.code.grpc.songpoem.GenerationSongGrpcControllerGrpc;
import abopijservice.code.grpc.songpoem.PoemGenerationRequest;
import abopijservice.code.grpc.songpoem.PoemGenerationResponse;
import abopijservice.code.grpc.songpoem.SongGenerationRequest;
import abopijservice.code.grpc.songpoem.SongGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SongByPoemAutoGenerationService {

    private final GenerationPoemsGrpcControllerGrpc.GenerationPoemsGrpcControllerBlockingStub poemGenerationStub;
    private final GenerationSongGrpcControllerGrpc.GenerationSongGrpcControllerBlockingStub songGenerationStub;

    public SongByPoemAutoGenerationResponse generate(SongByPoemAutoGenerationRequest request) {
        validate(request);

        PoemGenerationResponse poemPreparation = poemGenerationStub.generate(
                PoemGenerationRequest.newBuilder()
                        .setChatId(request.chatId() == null ? "" : request.chatId().toString())
                        .setPrompt(valueOrEmpty(request.prompt()))
                        .setWords(request.words())
                        .build()
        );
        SongGenerationResponse songGeneration = songGenerationStub.generate(
                SongGenerationRequest.newBuilder()
                        .setPoem(poemPreparation.getPoem())
                        .setStyles(poemPreparation.getStyles())
                        .build()
        );

        return new SongByPoemAutoGenerationResponse(
                parseUuid(poemPreparation.getChatId()),
                poemPreparation.getTitle(),
                poemPreparation.getPoem(),
                poemPreparation.getStyles(),
                songGeneration.getMinioKey(),
                StringUtils.hasText(songGeneration.getMinioKey())
                        ? List.of(songGeneration.getMinioKey())
                        : List.of()
        );
    }

    private static void validate(SongByPoemAutoGenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (!StringUtils.hasText(request.words())) {
            throw new IllegalArgumentException("words/original_poem is required");
        }
    }

    private static UUID parseUuid(String value) {
        return StringUtils.hasText(value) ? UUID.fromString(value) : null;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

}
