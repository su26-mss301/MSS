package wardrobe.project.com.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class AwsCognitoConfig {

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient(
            @Value("${aws.cognito.region}") String region
    ) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }
}