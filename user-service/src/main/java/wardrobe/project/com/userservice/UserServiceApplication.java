package wardrobe.project.com.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import wardrobe.project.com.userservice.config.properties.DefaultAdminProperties;

import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties(DefaultAdminProperties.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
