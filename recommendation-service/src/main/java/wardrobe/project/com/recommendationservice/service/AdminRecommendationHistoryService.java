package wardrobe.project.com.recommendationservice.service;

import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryPageResponse;

public interface AdminRecommendationHistoryService {

    RecommendationHistoryPageResponse getHistory(
            String type,
            int page,
            int size,
            String sort
    );
}
