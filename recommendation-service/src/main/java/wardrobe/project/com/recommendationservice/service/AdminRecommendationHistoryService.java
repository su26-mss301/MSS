package wardrobe.project.com.recommendationservice.service;

import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryDetailDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryPageResponse;

import java.util.UUID;

public interface AdminRecommendationHistoryService {

    RecommendationHistoryPageResponse getHistory(
            String type,
            int page,
            int size,
            String sort
    );

    RecommendationHistoryDetailDTO getDetail(UUID recommendationId);
}
