package abopijservice.code.songpoemautogeneration.infrastructure.redis;

import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPooled;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
public class RedisChatMemoryConfiguration {

    @Bean(destroyMethod = "close")
    JedisPooled jedisPooled(RedisChatMemoryProperties properties) {
        return new JedisPooled(properties.getHost(), properties.getPort());
    }

    @Bean
    RedisChatMemoryRepository redisChatMemoryRepository(
            JedisPooled jedisPooled,
            RedisChatMemoryProperties properties
    ) {
        var builder = RedisChatMemoryRepository.builder()
                .jedisClient(jedisPooled)
                .initializeSchema(properties.isInitializeSchema())
                .maxConversationIds(properties.getMaxConversationIds())
                .maxMessagesPerConversation(properties.getMaxMessagesPerConversation());

        if (StringUtils.hasText(properties.getIndexName())) {
            builder.indexName(properties.getIndexName());
        }

        if (StringUtils.hasText(properties.getKeyPrefix())) {
            builder.keyPrefix(properties.getKeyPrefix());
        }

        if (properties.getTimeToLive() != null) {
            builder.timeToLive(properties.getTimeToLive());
        }

        if (properties.getMetadataFields() != null && !properties.getMetadataFields().isEmpty()) {
            builder.metadataFields(properties.getMetadataFields());
        }

        return builder.build();
    }
}
