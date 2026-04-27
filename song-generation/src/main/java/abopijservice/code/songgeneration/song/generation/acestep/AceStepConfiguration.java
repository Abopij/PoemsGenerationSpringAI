package abopijservice.code.songgeneration.song.generation.acestep;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AceStepProperties.class)
public class AceStepConfiguration {

    @Bean
    protected RestClient aceStepRestClient(AceStepProperties properties) {
        ReactorClientHttpRequestFactory factory = new ReactorClientHttpRequestFactory(
                HttpClient
                        .create()
                        .responseTimeout(
                                Duration.ofSeconds(
                                        properties.getTimeoutSeconds()
                                )
                        )
        );

        return RestClient.builder()
                .baseUrl(properties.normalizedBaseUrl())
                .requestFactory(factory)
                .defaultHeader(
                        "Accept",
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.getApiKey())) {
                        headers.setBearerAuth(properties.getApiKey());
                    }
                })
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            byte[] body = response
                                    .getBody()
                                    .readAllBytes();

                            String raw = new String(body, StandardCharsets.UTF_8);

                            throw new IllegalStateException(
                                    "ACE-Step API error [HTTP "
                                            + response.getStatusCode() + "]: "
                                            + raw
                            );
                })
                .build();
    }
}
