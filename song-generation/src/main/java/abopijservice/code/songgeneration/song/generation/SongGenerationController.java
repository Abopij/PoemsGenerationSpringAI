package abopijservice.code.songgeneration.song.generation;

import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import abopijservice.code.songgeneration.song.generation.response.SongGenerationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/songs/")
@RequiredArgsConstructor
public class SongGenerationController {

    private final SongGenerationService songGenerationService;

    @PostMapping("generate")
    public SongGenerationResponse generateSong(@RequestBody AceStepGenerationRequest request) {
        return songGenerationService.generateSong(request);
    }
}
