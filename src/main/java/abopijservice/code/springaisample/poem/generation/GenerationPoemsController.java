package abopijservice.code.springaisample.poem.generation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/poems/")
@RequiredArgsConstructor
public class GenerationPoemsController {

    private final AiGenerationPoemsService generationPoemsService;

    @PostMapping("generate")
    public GenerationPoemsResponse generatePoems(@RequestBody GenerationPoemsRequest request) {
        return generationPoemsService.generatePoem(request);
    }

}
