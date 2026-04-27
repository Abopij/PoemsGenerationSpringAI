package abopijservice.code.songpoemautogeneration;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auto/")
@RequiredArgsConstructor
public class SongByPoemAutoGenerationController {

    private final SongByPoemAutoGenerationService service;

    @PostMapping("generate")
    public SongByPoemAutoGenerationResponse generateSongByPoemAutoGeneration(
            @RequestBody SongByPoemAutoGenerationRequest request
    ) {

    }
}
