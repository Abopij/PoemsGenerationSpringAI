package abopijservice.code.springaisample.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiChatRepo extends JpaRepository<AiChat, UUID> {
}
