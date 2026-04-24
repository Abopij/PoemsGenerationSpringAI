package abopijservice.code.poemgeneration.poem.generation;

import java.util.UUID;

public record GenerationPoemsRequest(
        UUID chatId,
        String prompt,
        GenerationLanguage generationLanguage
) {}
