package wardrobe.project.com.wardrobeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingItemResponseDTO {
    private UUID itemId;
    private UUID zoneId;
    private UUID categoryId;
    private UUID imageId;
    private String itemName;
    private String dominantColor;
    private String style;
    private Float confidenceScore;
    private LocalDateTime createdAt;
}
