package wardrobe.project.com.storageservice;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class StorageServiceApplication {

    public static void main(String[] args) {
        // Fix for Postgres >= 14 removing Asia/Saigon
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(StorageServiceApplication.class, args);
    }

}
