package wardrobe.project.com.recommendationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.recommendationservice.dto.response.ApiResponse;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationResponseDTO;
import wardrobe.project.com.recommendationservice.service.RecommendationServiceImpl;

import java.util.UUID;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationServiceImpl recommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getPersonalRecommendation(@RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Personal recommendation fetched successfully")
                .data(recommendationService.generateContentBased(userId))
                .build());
    }

    @GetMapping("/event")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getEventRecommendation(
            @RequestParam UUID userId,
            @RequestParam String eventType) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Event recommendation fetched successfully")
                .data(recommendationService.generateEventBased(userId, eventType))
                .build());
    }

    @GetMapping("/group")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getGroupRecommendation(
            @RequestParam UUID userId,
            @RequestParam UUID groupId) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Group recommendation fetched successfully")
                .data(recommendationService.generateCollaborative(userId, groupId))
                .build());
    }
}