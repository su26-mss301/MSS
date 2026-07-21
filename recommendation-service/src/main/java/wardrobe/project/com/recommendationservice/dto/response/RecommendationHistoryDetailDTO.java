package wardrobe.project.com.recommendationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationHistoryDetailDTO {

    private UUID recommendationId;
    private UUID userId;
    private String recommendationType;
    private String eventType;
    private Float recommendationScore;
    private LocalDateTime generatedAt;
    private OutfitResponseDTO outfit;
    private UUID groupId;
    private String groupName;
    private List<String> groupStyles;
    private List<RecommendationMemberOutfitDTO> members;
}
