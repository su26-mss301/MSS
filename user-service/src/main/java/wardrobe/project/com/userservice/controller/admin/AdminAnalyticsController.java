package wardrobe.project.com.userservice.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.response.admin.AnalyticsSummaryResponse;
import wardrobe.project.com.userservice.service.admin.AdminAnalyticsService;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> getSummary(
            @RequestParam(defaultValue = "week") String granularity
    ) {
        return ApiResponse.success(adminAnalyticsService.getSummary(granularity));
    }
}
