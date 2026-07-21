package wardrobe.project.com.recommendationservice.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingItemExternalDTO {
    private UUID itemId;
    private UUID zoneId;
    private CategoryDTO category;
    private UUID imageId;
    private String itemName;
    private String dominantColor;
    private String style;
    private Float confidenceScore;
    private UUID sharedByUserId;

    public UUID getCategoryId() {
        return category != null ? category.getCategoryId() : null;
    }
}