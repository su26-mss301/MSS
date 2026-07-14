package wardrobe.project.com.wardrobeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAnalyticsResponseDTO {
    private String granularity;
    private String from;
    private String to;
    private long totalItems;
    private List<CategoryAnalyticsItemDTO> categories;
}
