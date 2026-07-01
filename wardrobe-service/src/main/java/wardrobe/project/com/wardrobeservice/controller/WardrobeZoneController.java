package wardrobe.project.com.wardrobeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeZoneCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeZoneUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ApiResponse;
import wardrobe.project.com.wardrobeservice.dto.response.WardrobeZoneResponseDTO;
import wardrobe.project.com.wardrobeservice.service.WardrobeZoneService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wardrobe-zones")
@RequiredArgsConstructor
@Tag(name = "Wardrobe Zone", description = "Wardrobe Zone management APIs")
@PreAuthorize("hasAuthority('ROLE_USER')")
public class WardrobeZoneController {

    private final WardrobeZoneService wardrobeZoneService;

    @PostMapping
    @Operation(summary = "Create a new wardrobe zone")
    public ResponseEntity<ApiResponse<WardrobeZoneResponseDTO>> createZone(@Valid @RequestBody WardrobeZoneCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<WardrobeZoneResponseDTO>builder()
                .success(true)
                .message("Wardrobe zone created successfully")
                .data(wardrobeZoneService.createZone(request))
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a wardrobe zone by WardZoneID For User, nghĩa là lấy cụ thể 1 ngăn tủ")
    public ResponseEntity<ApiResponse<WardrobeZoneResponseDTO>> getZoneById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<WardrobeZoneResponseDTO>builder()
                .success(true)
                .message("Wardrobe zone fetched successfully")
                .data(wardrobeZoneService.getZoneById(id))
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all wardrobe zones For Admin")
    public ResponseEntity<ApiResponse<List<WardrobeZoneResponseDTO>>> getAllZones() {
        return ResponseEntity.ok(ApiResponse.<List<WardrobeZoneResponseDTO>>builder()
                .success(true)
                .message("All wardrobe zones fetched successfully")
                .data(wardrobeZoneService.getAllZones())
                .build());
    }

    @GetMapping("/wardrobe/{wardrobeId}")
    @Operation(summary = "Get all zones in a specific wardrobe, kiểm tra full tủ luôn")
    public ResponseEntity<ApiResponse<List<WardrobeZoneResponseDTO>>> getZonesByWardrobeId(@PathVariable UUID wardrobeId) {
        return ResponseEntity.ok(ApiResponse.<List<WardrobeZoneResponseDTO>>builder()
                .success(true)
                .message("Wardrobe zones fetched successfully")
                .data(wardrobeZoneService.getZonesByWardrobeId(wardrobeId))
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing wardrobe zone")
    public ResponseEntity<ApiResponse<WardrobeZoneResponseDTO>> updateZone(@PathVariable UUID id, @Valid @RequestBody WardrobeZoneUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.<WardrobeZoneResponseDTO>builder()
                .success(true)
                .message("Wardrobe zone updated successfully")
                .data(wardrobeZoneService.updateZone(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a wardrobe zone")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable UUID id) {
        wardrobeZoneService.deleteZone(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Wardrobe zone deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search wardrobe zones by name")
    public ResponseEntity<ApiResponse<List<WardrobeZoneResponseDTO>>> searchZones(
            @RequestParam(required = false) UUID wardrobeId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.<List<WardrobeZoneResponseDTO>>builder()
                .success(true)
                .message("Search results fetched successfully")
                .data(wardrobeZoneService.searchZones(wardrobeId, keyword))
                .build());
    }
}
