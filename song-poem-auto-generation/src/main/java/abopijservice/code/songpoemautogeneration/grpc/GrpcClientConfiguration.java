package abopijservice.code.songpoemautogeneration.grpc;

import abopijservice.code.grpc.songpoem.GenerationPoemsGrpcControllerGrpc;
import abopijservice.code.grpc.songpoem.GenerationSongGrpcControllerGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration(proxyBeanMethods = false)
public class GrpcClientConfiguration {

    @Bean
    GenerationPoemsGrpcControllerGrpc.GenerationPoemsGrpcControllerBlockingStub poemGenerationStub(
            GrpcChannelFactory channels
    ) {
        return GenerationPoemsGrpcControllerGrpc.newBlockingStub(
                channels.createChannel("poem-generation")
        );
    }

    @Bean
    GenerationSongGrpcControllerGrpc.GenerationSongGrpcControllerBlockingStub songGenerationStub(
            GrpcChannelFactory channels
    ) {
        return GenerationSongGrpcControllerGrpc.newBlockingStub(
                channels.createChannel("song-generation")
        );
    }
}
