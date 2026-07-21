package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.response.AnalyticsSummaryResponse;

public interface AdminAnalyticsService {
    AnalyticsSummaryResponse getSummary(String granularity);
}
