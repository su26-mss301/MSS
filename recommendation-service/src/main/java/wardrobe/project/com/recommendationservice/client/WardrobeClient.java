package wardrobe.project.com.recommendationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import wardrobe.project.com.recommendationservice.dto.ClothingItemDto;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "wardrobe-service")
public interface WardrobeClient {

    // Đường dẫn này phải khớp với API định nghĩa bên WardrobeController
    @GetMapping("/api/wardrobe/users/{userId}/clothes")
    List<ClothingItemDto> getUserClothes(@PathVariable("userId") UUID userId);
}