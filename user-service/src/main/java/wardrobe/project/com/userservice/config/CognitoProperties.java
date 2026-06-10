package wardrobe.project.com.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "aws.cognito")
public class CognitoProperties {

    private String region;
    private String userPoolId;
    private String clientId;
    private String defaultGroup;

}