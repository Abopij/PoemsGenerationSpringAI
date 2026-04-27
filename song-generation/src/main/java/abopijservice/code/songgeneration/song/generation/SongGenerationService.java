package abopijservice.code.songgeneration.song.generation;

import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import abopijservice.code.songgeneration.song.generation.response.SongGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SongGenerationService {

    private final AceStepClient aceStepClient;

    public SongGenerationResponse generateSong(AceStepGenerationRequest request) {
        if (!StringUtils.hasText(request.caption()) && !StringUtils.hasText(request.lyrics())) {
            throw new IllegalArgumentException("Either prompt or lyrics must be provided");
        }

        return aceStepClient.generateSong(request);
    }
}
