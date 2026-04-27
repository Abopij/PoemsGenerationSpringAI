package abopijservice.code.poemgeneration.songpoem;

import abopijservice.code.poemgeneration.chat.AiChat;
import abopijservice.code.poemgeneration.chat.AiChatService;
import abopijservice.code.poemgeneration.chat.message.AiChatMessage;
import abopijservice.code.poemgeneration.chat.message.AiChatMessageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class SongPoemPreparationService {

    private final ChatClient chatClient;
    private final AiChatService aiChatService;
    private final AiChatMessageService aiChatMessageService;

    public SongPoemPreparationService(
            VectorStore vectorStore,
            RedisChatMemoryRepository chatMemoryRepository,
            ChatClient.Builder builder,
            AiChatService aiChatService,
            AiChatMessageService aiChatMessageService
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

        this.aiChatService = aiChatService;
        this.aiChatMessageService = aiChatMessageService;
    }

    public SongPoemPreparationResponse prepare(SongPoemPreparationRequest request) {
        validate(request);

        AiChat chat = resolveChat(request.chatId());
        String poem = completePoem(request, chat.getId());
        String styles = generateStyles(request.prompt(), poem, chat.getId());

        if (!StringUtils.hasText(chat.getTitle())) {
            chat.setTitle(createTitle(chat.getId()));
            aiChatService.update(chat);
        }

        aiChatMessageService.save(
                AiChatMessage.builder()
                        .prompt(request.words())
                        .response(poem + "\n\nStyles:\n" + styles)
                        .aiChat(chat)
                        .build()
        );

        return new SongPoemPreparationResponse(chat.getId(), chat.getTitle(), poem, styles);
    }

    private AiChat resolveChat(UUID chatId) {
        if (chatId == null) {
            return aiChatService.save(new AiChat());
        }

        AiChat chat = aiChatService.findById(chatId);
        return chat == null ? aiChatService.save(new AiChat()) : chat;
    }

    private String completePoem(SongPoemPreparationRequest request, UUID chatId) {
        String userPrompt = StringUtils.hasText(request.prompt())
                ? request.prompt()
                : "Finish and polish the unfinished poem.";

        return chatClient
                .prompt(userPrompt + "\n\nUnfinished poem:\n" + request.words())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId.toString()))
                .system(systemPrompt -> systemPrompt
                        .text("You are a professional poet and songwriter. Complete the unfinished poem, "
                                + "preserve the user's images and rhythm, improve weak lines only when needed. "
                                + "Return only the finished lyrics/poem, no explanations."))
                .call()
                .content();
    }

    private String generateStyles(String userPrompt, String poem, UUID chatId) {
        String prompt = StringUtils.hasText(userPrompt)
                ? userPrompt
                : "Create the best style prompt for music generation.";

        return chatClient
                .prompt(prompt + "\n\nFinished lyrics/poem:\n" + poem)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId.toString()))
                .system(systemPrompt -> systemPrompt
                        .text("You are a professional music producer with 20 years of experience. "
                                + "Create a concise music generation prompt up to 950 characters. "
                                + "Describe genre, mood, tempo feel, vocal delivery, instruments, dynamics, "
                                + "mix accents and arrangement. Return only the prompt."))
                .call()
                .content();
    }

    private String createTitle(UUID chatId) {
        return chatClient
                .prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId.toString()))
                .system(systemPrompt -> systemPrompt
                        .text("Create shortest title, less or equals 5 words, for this song/poem chat."))
                .call()
                .content();
    }

    private static void validate(SongPoemPreparationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        if (!StringUtils.hasText(request.words())) {
            throw new IllegalArgumentException("words/original_poem is required");
        }
    }
}
