package abopijservice.code.songpoemautogeneration.songpoem;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.UUID;

public record SongByPoemAutoGenerationRequest(
        @JsonAlias({"chat_id", "chatId"}) UUID chatId,
        String prompt,
        @JsonAlias({"words", "original_poem", "originalPoem"}) String words
) {
}
