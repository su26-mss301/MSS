package wardrobe.project.com.recommendationservice.service;

import wardrobe.project.com.recommendationservice.dto.response.AnalyticsSummaryResponse;

public interface AdminAnalyticsService {
    AnalyticsSummaryResponse getSummary(String granularity);
}
