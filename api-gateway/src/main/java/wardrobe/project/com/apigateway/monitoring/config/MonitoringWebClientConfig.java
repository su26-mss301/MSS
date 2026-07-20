package wardrobe.project.com.apigateway.monitoring.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class MonitoringWebClientConfig {

    @Bean
    public WebClient monitoringWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        5_000
                )
                .responseTimeout(Duration.ofSeconds(15));

        return builder
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .build();
    }
}