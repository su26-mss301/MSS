package wardrobe.project.com.userservice.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.default-admin")
public class DefaultAdminProperties {

    private boolean enabled;

    private String email;

    private String username;

    private String password;

    private String fullName;
}