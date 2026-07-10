package wardrobe.project.com.wardrobeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ShareClothingItemRequest {

    @NotNull(message = "clothingItemId is required")
    private UUID clothingItemId;

    @NotBlank(message = "groupId is required")
    private String groupId;
}
