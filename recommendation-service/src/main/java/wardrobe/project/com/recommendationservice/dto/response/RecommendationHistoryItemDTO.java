package wardrobe.project.com.recommendationservice.dto.response;

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
public class RecommendationHistoryItemDTO {

    private UUID recommendationId;
    private UUID userId;
    private String outfitName;
    private String description;
    private int itemCount;
    private float recommendationScore;
    private String eventType;
    private String recommendationType;
    private LocalDateTime generatedAt;
}
