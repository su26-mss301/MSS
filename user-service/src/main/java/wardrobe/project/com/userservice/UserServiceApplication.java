package wardrobe.project.com.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        System.out.println("ACCESS_KEY = " + System.getenv("AWS_ACCESS_KEY_ID"));
        System.out.println("SECRET_KEY = " + System.getenv("AWS_SECRET_ACCESS_KEY"));
        System.out.println("REGION = " + System.getenv("AWS_REGION"));
        SpringApplication.run(UserServiceApplication.class, args);
    }

}
