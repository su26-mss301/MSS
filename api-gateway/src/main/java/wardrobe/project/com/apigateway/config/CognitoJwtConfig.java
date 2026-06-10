package wardrobe.project.com.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
@EnableConfigurationProperties(CognitoProperties.class)
public class CognitoJwtConfig {

    @Bean
    public ReactiveJwtDecoder cognitoJwtDecoder(CognitoProperties cognitoProperties) {
        return NimbusReactiveJwtDecoder
                .withJwkSetUri(cognitoProperties.getJwkSetUri())
                .build();
    }
}