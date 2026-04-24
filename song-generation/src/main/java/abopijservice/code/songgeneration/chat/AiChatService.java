package abopijservice.code.songgeneration.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiChatRepo repo;

    public AiChat save(AiChat chat) {
        return repo.save(chat);
    }

    public AiChat update(AiChat chat) {
        return repo.saveAndFlush(chat);
    }

    public AiChat findById(UUID id) {
        return repo.findById(id).orElse(null);
    }

}
