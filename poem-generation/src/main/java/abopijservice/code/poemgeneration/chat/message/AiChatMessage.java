package abopijservice.code.poemgeneration.chat.message;

import abopijservice.code.poemgeneration.chat.AiChat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = Integer.MAX_VALUE)
    private String prompt;

    @Column(length = Integer.MAX_VALUE)
    private String response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private AiChat aiChat;

}
