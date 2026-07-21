package wardrobe.project.com.recommendationservice.config;

import com.wardrobe.common.auth.AuthContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class AuthContextConfig {

    @Bean
    public AuthContextFilter authContextFilter() {
        return new AuthContextFilter(List.of(
                // Public recommendation APIs (context-path stripped in filter)
                "/generate/**",
                "/user/**",
                "/*",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info"
        ));
    }
}