package wardrobe.project.com.wardrobeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.project.com.wardrobeservice.dto.response.AnalyticsSummaryResponse;
import wardrobe.project.com.wardrobeservice.dto.response.ApiResponse;
import wardrobe.project.com.wardrobeservice.service.AdminAnalyticsService;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.<AnalyticsSummaryResponse>builder()
                .success(true)
                .message("Analytics summary fetched successfully")
                .data(adminAnalyticsService.getSummary())
                .build());
    }
}
