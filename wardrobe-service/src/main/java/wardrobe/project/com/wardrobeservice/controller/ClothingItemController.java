package wardrobe.project.com.wardrobeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ApiResponse;
import wardrobe.project.com.wardrobeservice.dto.response.ClothingItemResponseDTO;
import wardrobe.project.com.wardrobeservice.service.ClothingItemService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clothing-items")
@RequiredArgsConstructor
@Tag(name = "Clothing Item", description = "Clothing Item management APIs")
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
public class ClothingItemController {

    private final ClothingItemService clothingItemService;

    @PostMapping
    @Operation(summary = "Create a new clothing item")
    public ResponseEntity<ApiResponse<ClothingItemResponseDTO>> createClothingItem(@Valid @RequestBody ClothingItemCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ClothingItemResponseDTO>builder()
                .success(true)
                .message("Clothing item created successfully")
                .data(clothingItemService.createClothingItem(request))
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a clothing item by ID")
    public ResponseEntity<ApiResponse<ClothingItemResponseDTO>> getClothingItemById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<ClothingItemResponseDTO>builder()
                .success(true)
                .message("Clothing item fetched successfully")
                .data(clothingItemService.getClothingItemById(id))
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all clothing items of current user")
    public ResponseEntity<ApiResponse<List<ClothingItemResponseDTO>>> getAllClothingItems(
            @RequestHeader("X-Auth-User-Id") String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(ApiResponse.<List<ClothingItemResponseDTO>>builder()
                .success(true)
                .message("Clothing items of current user fetched successfully")
                .data(clothingItemService.getAllClothingItems(userId))
                .build());
    }

    @GetMapping("/zone/{zoneId}")
    @Operation(summary = "Get all items in a specific zone")
    public ResponseEntity<ApiResponse<List<ClothingItemResponseDTO>>> getItemsByZoneId(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(ApiResponse.<List<ClothingItemResponseDTO>>builder()
                .success(true)
                .message("Items by zone fetched successfully")
                .data(clothingItemService.getItemsByZoneId(zoneId))
                .build());
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get all items in a specific category")
    public ResponseEntity<ApiResponse<List<ClothingItemResponseDTO>>> getItemsByCategoryId(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.<List<ClothingItemResponseDTO>>builder()
                .success(true)
                .message("Items by category fetched successfully")
                .data(clothingItemService.getItemsByCategoryId(categoryId))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing clothing item")
    public ResponseEntity<ApiResponse<ClothingItemResponseDTO>> updateClothingItem(@PathVariable UUID id, @Valid @RequestBody ClothingItemUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.<ClothingItemResponseDTO>builder()
                .success(true)
                .message("Clothing item updated successfully")
                .data(clothingItemService.updateClothingItem(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a clothing item")
    public ResponseEntity<ApiResponse<Void>> deleteClothingItem(@PathVariable UUID id) {
        clothingItemService.deleteClothingItem(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Clothing item deleted successfully")
                .data(null)
                .build());
    }
}
