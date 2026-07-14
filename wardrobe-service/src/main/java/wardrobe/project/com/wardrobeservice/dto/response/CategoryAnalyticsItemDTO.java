package wardrobe.project.com.wardrobeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAnalyticsItemDTO {
    private UUID categoryId;
    private String categoryName;
    private String description;
    private long count;
    private double percentage;
}
