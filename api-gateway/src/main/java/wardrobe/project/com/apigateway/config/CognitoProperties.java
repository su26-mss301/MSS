package wardrobe.project.com.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cognito")
@Getter
@Setter
public class CognitoProperties {

    private String region;
    private String userPoolId;
    private String clientId;
    private String issuerUri;
    private String jwkSetUri;


}