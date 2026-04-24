package abopijservice.code.poemgeneration.chat.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatMessageService {

    private final AiChatMessageRepo repo;

    public AiChatMessage save(final AiChatMessage message) {
        return repo.save(message);
    }

    public AiChatMessage update(final AiChatMessage message) {
        return repo.saveAndFlush(message);
    }

    public void delete(final AiChatMessage message) {
        repo.delete(message);
    }

    public List<AiChatMessage> findAll(final UUID chatId) {
        return repo.findAllByAiChat_Id(chatId);
    }

}
