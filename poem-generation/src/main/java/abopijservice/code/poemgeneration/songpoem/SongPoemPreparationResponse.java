package abopijservice.code.poemgeneration.songpoem;

import java.util.UUID;

public record SongPoemPreparationResponse(
        UUID chatId,
        String title,
        String poem,
        String styles
) {
}
