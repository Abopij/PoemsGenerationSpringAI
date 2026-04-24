package abopijservice.code.poemgeneration.poem.generation;

import abopijservice.code.poemgeneration.chat.AiChat;
import abopijservice.code.poemgeneration.chat.AiChatService;
import abopijservice.code.poemgeneration.chat.message.AiChatMessage;
import abopijservice.code.poemgeneration.chat.message.AiChatMessageService;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class AiGenerationPoemsService {

    private final ChatClient chatClient;
    private final AiChatService aiChatService;
    private final AiChatMessageService aiChatMessageService;


    public AiGenerationPoemsService(
            VectorStore vectorStore,
            RedisChatMemoryRepository chatMemoryRepository,
            ChatClient.Builder builder,
            AiChatMessageService aiChatMessageService,
            AiChatService aiChatService
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

        this.aiChatMessageService = aiChatMessageService;
        this.aiChatService = aiChatService;
    }

    public GenerationPoemsResponse generatePoem(@NotNull GenerationPoemsRequest generationPoemsRequest) {
        AiChat chat = generationPoemsRequest.chatId() == null ?
                aiChatService.save(new AiChat())
                :
                aiChatService
                        .findById(
                                generationPoemsRequest.chatId()
                        );

        String generationResult = chatClient
                .prompt(generationPoemsRequest.prompt())
                .advisors(advisor -> advisor
                        .param(
                                ChatMemory.CONVERSATION_ID, chat.getId().toString()
                        )
                )
                .system(
                        systemPrompt -> systemPrompt
                                .text(
                                        String.format("You are a professional poet with 20 years of experience, " +
                                                        "your rhyme is always very structured, but occasionally free. " +
                                                        "Your main feature is that you write poems with excellent rhyme (using a company generator). " +
                                                        "Your main task is to write the best possible poem in %s language. " +
                                                        "Write only a poem in the reply.",
                                                generationPoemsRequest
                                                        .generationLanguage()
                                                        .toString()
                                        )
                                )
                )
                .call()
                .content();

        if (chat.getTitle() == null) {
            chat.setTitle(
                    getTitleForChat(
                            chat.getId()
                    )
            );
            aiChatService.update(chat);
        }
        aiChatMessageService.save(
                AiChatMessage.builder()
                        .prompt(generationPoemsRequest.prompt())
                        .response(generationResult)
                        .aiChat(chat)
                        .build()
        );


        return new GenerationPoemsResponse(
                chat.getId(),
                chat.getTitle(),
                generationResult
        );
    }

    private String getTitleForChat(final UUID chatId) {
        return chatClient
                .prompt()
                .advisors(advisor -> advisor
                        .param(
                                ChatMemory.CONVERSATION_ID, chatId.toString()
                        )
                )
                .system(
                        systemPrompt -> systemPrompt
                                .text("Create shortest title (less or equals 5 words) for chat with text")
                )
                .call()
                .content();
    }
}
