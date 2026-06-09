package wardrobe.project.com.wardrobeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ApiResponse;
import wardrobe.project.com.wardrobeservice.dto.response.WardrobeResponseDTO;
import wardrobe.project.com.wardrobeservice.service.WardrobeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wardrobes")
@RequiredArgsConstructor
@Tag(name = "Wardrobe", description = "Wardrobe management APIs")
public class WardrobeController {

    private final WardrobeService wardrobeService;

    @PostMapping
    @Operation(summary = "Create a new wardrobe")
    public ResponseEntity<ApiResponse<WardrobeResponseDTO>> createWardrobe(@Valid @RequestBody WardrobeCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<WardrobeResponseDTO>builder()
                .success(true)
                .message("Wardrobe created successfully")
                .data(wardrobeService.createWardrobe(request))
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a wardrobe by ID")
    public ResponseEntity<ApiResponse<WardrobeResponseDTO>> getWardrobeById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<WardrobeResponseDTO>builder()
                .success(true)
                .message("Wardrobe fetched successfully")
                .data(wardrobeService.getWardrobeById(id))
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all wardrobes")
    public ResponseEntity<ApiResponse<List<WardrobeResponseDTO>>> getAllWardrobes() {
        return ResponseEntity.ok(ApiResponse.<List<WardrobeResponseDTO>>builder()
                .success(true)
                .message("All wardrobes fetched successfully")
                .data(wardrobeService.getAllWardrobes())
                .build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all wardrobes for a specific user")
    public ResponseEntity<ApiResponse<List<WardrobeResponseDTO>>> getWardrobesByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.<List<WardrobeResponseDTO>>builder()
                .success(true)
                .message("User wardrobes fetched successfully")
                .data(wardrobeService.getWardrobesByUserId(userId))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing wardrobe")
    public ResponseEntity<ApiResponse<WardrobeResponseDTO>> updateWardrobe(@PathVariable UUID id, @Valid @RequestBody WardrobeUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.<WardrobeResponseDTO>builder()
                .success(true)
                .message("Wardrobe updated successfully")
                .data(wardrobeService.updateWardrobe(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a wardrobe")
    public ResponseEntity<ApiResponse<Void>> deleteWardrobe(@PathVariable UUID id) {
        wardrobeService.deleteWardrobe(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Wardrobe deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search wardrobes by name")
    public ResponseEntity<ApiResponse<List<WardrobeResponseDTO>>> searchWardrobes(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.<List<WardrobeResponseDTO>>builder()
                .success(true)
                .message("Search results fetched successfully")
                .data(wardrobeService.searchWardrobes(keyword))
                .build());
    }
}