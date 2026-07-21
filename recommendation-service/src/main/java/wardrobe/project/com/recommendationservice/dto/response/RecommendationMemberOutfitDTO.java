package wardrobe.project.com.recommendationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationMemberOutfitDTO {

    private UUID userId;
    private String fullName;
    private Float recommendationScore;
    private OutfitResponseDTO outfit;
    private boolean creator;
}
