package wardrobe.project.com.recommendationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.recommendationservice.dto.response.ApiResponse;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationResponseDTO;
import wardrobe.project.com.recommendationservice.service.RecommendationServiceImpl;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping()
@PreAuthorize("hasAuthority('ROLE_USER')")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationServiceImpl recommendationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<RecommendationResponseDTO>>> getAllRecommendations(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.<List<RecommendationResponseDTO>>builder()
                .success(true)
                .message("All recommendations for user fetched successfully")
                .data(recommendationService.getAllByUserId(userId))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getRecommendationDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Recommendation detail fetched successfully")
                .data(recommendationService.getById(id))
                .build());
    }

    @GetMapping("/generate/personal")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getPersonalRecommendation(@RequestParam UUID userId) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Personal recommendation generated successfully")
                .data(recommendationService.generateContentBased(userId))
                .build());
    }

    @GetMapping("/generate/event")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getEventRecommendation(
            @RequestParam UUID userId,
            @RequestParam String eventType) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Event recommendation generated successfully")
                .data(recommendationService.generateEventBased(userId, eventType))
                .build());
    }

    @GetMapping("/generate/group")
    public ResponseEntity<ApiResponse<RecommendationResponseDTO>> getGroupRecommendation(
            @RequestParam UUID userId,
            @RequestParam UUID groupId) {
        return ResponseEntity.ok(ApiResponse.<RecommendationResponseDTO>builder()
                .success(true)
                .message("Group recommendation generated successfully")
                .data(recommendationService.generateCollaborative(userId, groupId))
                .build());
    }
}