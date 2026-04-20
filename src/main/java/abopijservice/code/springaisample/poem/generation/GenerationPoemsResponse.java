package abopijservice.code.springaisample.poem.generation;

import java.util.UUID;

public record GenerationPoemsResponse(
        UUID chatId,
        String title,
        String responseGeneration
) {}