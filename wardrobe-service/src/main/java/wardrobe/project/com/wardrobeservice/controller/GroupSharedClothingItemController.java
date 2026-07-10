package wardrobe.project.com.wardrobeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.wardrobeservice.dto.request.ShareClothingItemRequest;
import wardrobe.project.com.wardrobeservice.dto.response.ApiResponse;
import wardrobe.project.com.wardrobeservice.dto.response.SharedClothingItemResponse;
import wardrobe.project.com.wardrobeservice.service.GroupSharedClothingItemService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/clothing-items/shared")
@RequiredArgsConstructor
@Tag(name = "Group Shared Clothing Item", description = "Chia sẻ trang phục vào nhóm bạn")
@PreAuthorize("hasAuthority('ROLE_USER')")
public class GroupSharedClothingItemController {

    private final GroupSharedClothingItemService groupSharedService;

    /**
     * POST /api/v1/wardrobe/clothing-items/shared
     */
    @PostMapping
    @Operation(summary = "Chia sẻ một trang phục vào nhóm bạn")
    public ResponseEntity<ApiResponse<SharedClothingItemResponse>> shareItem(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @Valid @RequestBody ShareClothingItemRequest request
    ) {
        UUID userId = UUID.fromString(userIdStr);
        SharedClothingItemResponse response = groupSharedService.shareItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<SharedClothingItemResponse>builder()
                        .success(true)
                        .message("Chia sẻ trang phục thành công")
                        .data(response)
                        .build());
    }

    /**
     * GET /api/v1/wardrobe/clothing-items/shared/group/{groupId}
     * Header X-Auth-User-Id dùng để tính likedByMe
     */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "Lấy danh sách trang phục đã chia sẻ vào nhóm")
    public ResponseEntity<ApiResponse<List<SharedClothingItemResponse>>> getSharedByGroup(
            @PathVariable String groupId,
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userIdStr
    ) {
        UUID currentUserId = userIdStr != null ? UUID.fromString(userIdStr) : null;
        List<SharedClothingItemResponse> items = groupSharedService.getSharedItemsByGroup(groupId, currentUserId);
        return ResponseEntity.ok(ApiResponse.<List<SharedClothingItemResponse>>builder()
                .success(true)
                .message("Lấy danh sách chia sẻ thành công")
                .data(items)
                .build());
    }

    /**
     * DELETE /api/v1/wardrobe/clothing-items/shared/{shareId}
     */
    @DeleteMapping("/{shareId}")
    @Operation(summary = "Hủy chia sẻ trang phục khỏi nhóm")
    public ResponseEntity<ApiResponse<Void>> unshareItem(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @PathVariable UUID shareId
    ) {
        UUID userId = UUID.fromString(userIdStr);
        groupSharedService.unshareItem(shareId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Hủy chia sẻ thành công")
                .data(null)
                .build());
    }

    /**
     * POST /api/v1/wardrobe/clothing-items/shared/{shareId}/like
     * Toggle like — nếu chưa like thì like, nếu đã like thì unlike.
     */
    @PostMapping("/{shareId}/like")
    @Operation(summary = "Toggle like/unlike một trang phục đã chia sẻ")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @RequestHeader("X-Auth-User-Id") String userIdStr,
            @PathVariable UUID shareId
    ) {
        UUID userId = UUID.fromString(userIdStr);
        boolean liked = groupSharedService.toggleLike(shareId, userId);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message(liked ? "Đã thích trang phục" : "Đã bỏ thích trang phục")
                .data(Map.of("liked", liked))
                .build());
    }
}
