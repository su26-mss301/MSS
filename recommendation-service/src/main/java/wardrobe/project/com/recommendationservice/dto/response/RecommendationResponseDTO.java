package wardrobe.project.com.recommendationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDTO {
    private UUID recommendationId;
    private UUID userId;
    private OutfitResponseDTO outfit;
    private float recommendationScore;
    private String eventType;
    private List<String> sources;
}