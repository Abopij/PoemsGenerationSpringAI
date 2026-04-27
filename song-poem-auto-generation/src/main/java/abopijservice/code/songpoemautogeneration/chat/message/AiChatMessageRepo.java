package abopijservice.code.songpoemautogeneration.chat.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiChatMessageRepo extends JpaRepository<AiChatMessage, UUID> {

    List<AiChatMessage> findAllByAiChat_Id(UUID chatId);
}
