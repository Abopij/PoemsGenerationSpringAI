package abopijservice.code.songpoemautogeneration.songpoem;

import java.util.List;
import java.util.UUID;

public record SongByPoemAutoGenerationResponse(
        UUID chatId,
        String title,
        String poem,
        String styles,
        String minioKey,
        List<String> minioKeys
) {}
