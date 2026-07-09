package wardrobe.project.com.wardrobeservice.config;

import com.wardrobe.common.auth.AuthContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AuthContextConfig {

    @Bean
    public AuthContextFilter authContextFilter() {
        return new AuthContextFilter(List.of(
                //những api muốn public
                //VD: "/auth/**"
        ));
    }
}