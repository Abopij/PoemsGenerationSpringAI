package abopijservice.code.songgeneration.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.ai.chat.memory.redis")
@Data
public class RedisChatMemoryProperties {
    private String host = "localhost";
    private int port = 6379;
    private String indexName;
    private String keyPrefix;
    private Duration timeToLive;
    private boolean initializeSchema = true;
    private int maxConversationIds = 1000;
    private int maxMessagesPerConversation = 1000;
    private List<Map<String, String>> metadataFields;
}
