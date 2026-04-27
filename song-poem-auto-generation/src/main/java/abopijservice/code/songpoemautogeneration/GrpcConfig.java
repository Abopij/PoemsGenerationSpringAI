package abopijservice.code.songpoemautogeneration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcConfig {

    @Bean
    SimpleGrpc.SimpleBlockingStub stub(GrpcChannelFactory channels) {
        return SimpleGrpc.newBlockingStub(
                channels.createChannel(
                        "local"
                )
        );
    }
}
