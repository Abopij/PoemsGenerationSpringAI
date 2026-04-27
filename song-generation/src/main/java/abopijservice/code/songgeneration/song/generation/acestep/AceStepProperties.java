package abopijservice.code.songgeneration.song.generation.acestep;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Data
@ConfigurationProperties(prefix = "ace-step")
public class AceStepProperties {

    private String baseUrl = "https://api.acemusic.ai";
    private String apiKey;
    private String model = "acemusic/acestep-v1.5-turbo";
    private long timeoutSeconds = 600;
    private Defaults defaults = new Defaults();

    public String normalizedBaseUrl() {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        return baseUrl
                .endsWith("/") ?
                    baseUrl
                            .substring(
                                    0,
                                    baseUrl.length() - 1
                            )
                    :
                    baseUrl;
    }

    public void requireApiKeyForAceCloud() {
        String normalizedBaseUrl = normalizedBaseUrl();
        if (StringUtils.hasText(apiKey)
                || !StringUtils.hasText(normalizedBaseUrl)
                || !normalizedBaseUrl.contains("api.acemusic.ai")) {
            return;
        }
        throw new IllegalStateException(
                "ACE Music API key is not configured. " +
                        "Set ACE_MUSIC_API_KEY " +
                        "or ace-step.api-key before calling "
                        + normalizedBaseUrl
                        + "/v1/chat/completions"
        );
    }

    @Data
    public static class Defaults {
        private String taskType = "text2music";
        private String audioFormat = "mp3";
        private String vocalLanguage = "en";
        private double temperature = 0.85;
        private double topP = 0.9;
        private boolean sampleMode = false;
        private boolean thinking = false;
        private boolean useFormat = false;
        private boolean useCotCaption = true;
        private boolean useCotLanguage = true;
        private double guidanceScale = 7.0;
        private int batchSize = 1;
    }
}
