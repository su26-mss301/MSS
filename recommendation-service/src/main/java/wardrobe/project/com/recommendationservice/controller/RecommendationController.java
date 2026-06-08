package wardrobe.project.com.recommendationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.service.RecommendationServiceImpl;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationServiceImpl recommendationService;

    /**
     * API 1: Lấy gợi ý chung dựa trên sở thích (Content-Based)
     * GET /api/v1/recommend?userId=...
     */
    @GetMapping
    public ResponseEntity<RecommendItem> getPersonalRecommendation(@RequestParam UUID userId) {
        RecommendItem result = recommendationService.generateContentBased(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * API 2: Lấy gợi ý theo tính chất sự kiện (Event-Based)
     * GET /api/v1/recommend/event?userId=...&eventType=wedding
     */
    @GetMapping("/event")
    public ResponseEntity<RecommendItem> getEventRecommendation(
            @RequestParam UUID userId,
            @RequestParam String eventType) {
        RecommendItem result = recommendationService.generateEventBased(userId, eventType);
        return ResponseEntity.ok(result);
    }

    /**
     * API 3: Lấy gợi ý theo nhóm bạn bè (Collaborative Filtering)
     * GET /api/v1/recommend/group?userId=...&groupId=...
     */
    @GetMapping("/group")
    public ResponseEntity<RecommendItem> getGroupRecommendation(
            @RequestParam UUID userId,
            @RequestParam UUID groupId) {
        RecommendItem result = recommendationService.generateCollaborative(userId, groupId);
        return ResponseEntity.ok(result);
    }
}