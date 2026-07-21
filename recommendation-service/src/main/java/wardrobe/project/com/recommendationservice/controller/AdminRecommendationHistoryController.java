package wardrobe.project.com.recommendationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wardrobe.project.com.recommendationservice.dto.response.ApiResponse;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryPageResponse;
import wardrobe.project.com.recommendationservice.service.AdminRecommendationHistoryService;

@RestController
@RequestMapping("/admin/history")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminRecommendationHistoryController {

    private final AdminRecommendationHistoryService adminRecommendationHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<RecommendationHistoryPageResponse>> getHistory(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.<RecommendationHistoryPageResponse>builder()
                .success(true)
                .message("Recommendation history fetched successfully")
                .data(adminRecommendationHistoryService.getHistory(type, page, size, sort))
                .build());
    }
}
