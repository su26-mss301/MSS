package wardrobe.project.com.userservice.service.admin;

import wardrobe.project.com.userservice.dto.response.admin.AnalyticsSummaryResponse;

public interface AdminAnalyticsService {
    AnalyticsSummaryResponse getSummary(String granularity);
}
