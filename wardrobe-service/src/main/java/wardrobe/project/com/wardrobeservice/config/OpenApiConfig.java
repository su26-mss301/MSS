package wardrobe.project.com.wardrobeservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI wardrobeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Wardrobe Service API")
                        .description("API documentation for the Wardrobe microservice in the Smart Wardrobe Management System")
                        .version("v1.0.0"));
    }
}
