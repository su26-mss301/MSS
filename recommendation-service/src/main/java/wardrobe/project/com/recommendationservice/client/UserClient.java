package wardrobe.project.com.recommendationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import wardrobe.project.com.recommendationservice.dto.UserProfileDto;

import java.util.UUID;

// Tên "user-service" phải khớp chính xác với spring.application.name của User Service trên Eureka
@FeignClient(name = "user-service")
public interface UserClient {

    // Đường dẫn này phải khớp với API được định nghĩa bên UserController
    @GetMapping("/api/users/{userId}/profile")
    UserProfileDto getUserProfile(@PathVariable("userId") UUID userId);
}