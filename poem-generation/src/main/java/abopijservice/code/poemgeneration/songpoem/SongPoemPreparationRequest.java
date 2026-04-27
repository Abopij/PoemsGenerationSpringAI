package abopijservice.code.poemgeneration.songpoem;

import java.util.UUID;

public record SongPoemPreparationRequest(
        UUID chatId,
        String prompt,
        String words
) {
}
