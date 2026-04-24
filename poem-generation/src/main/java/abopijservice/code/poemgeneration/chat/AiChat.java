package abopijservice.code.poemgeneration.chat;

import abopijservice.code.poemgeneration.chat.message.AiChatMessage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TimeZoneColumn;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class AiChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private String title;

    @OneToMany
    @JoinColumn(name = "chat_id")
    private List<AiChatMessage> messages;

    @TimeZoneColumn
    @CreationTimestamp
    private ZonedDateTime createdAt;

}
