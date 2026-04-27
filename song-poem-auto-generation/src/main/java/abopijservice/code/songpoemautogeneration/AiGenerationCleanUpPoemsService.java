package abopijservice.code.songpoemautogeneration;

import jakarta.validation.constraints.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;


@Service
public class AiGenerationCleanUpPoemsService {

    private final ChatClient chatClient;

    public AiGenerationCleanUpPoemsService(
            VectorStore vectorStore,
            RedisChatMemoryRepository chatMemoryRepository,
            ChatClient.Builder builder
    ) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(25)
                .build();

        this.chatClient = builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .build();
    }

    public String generateStylesSongs(@NotNull String originalPoem) {

        return chatClient
                .prompt(originalPoem)
                .system(
                        systemPrompt -> systemPrompt
                                .text(
                                        String.format(
                                                "You are a professional music producer with 20 years of experience " +
                                                        "using AI as the ultimate song generation tool," +
                                                        "Your styles for songs are always described clearly and in great detail (up to 950 characters)." +
                                                        "Your main feature is that you write the best prompta for music generation, " +
                                                        "always specify the volume and accents of the songs. " +
                                                        "Especially the volume and differentiation of musical instruments" +
                                                        "Your main task is to write the best possible prompta for the musical AI. "+
                                                        "In response, write only this prompt. Poems = \" %s \" ",
                                                originalPoem
                                        )


                                )
                )
                .call()
                .content();
    }
}
