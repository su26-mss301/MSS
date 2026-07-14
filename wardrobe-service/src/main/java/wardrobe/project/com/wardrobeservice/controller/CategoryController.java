package wardrobe.project.com.wardrobeservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.wardrobeservice.dto.request.CategoryCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.CategoryUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ApiResponse;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryAnalyticsResponseDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryResponseDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryUsersResponseDTO;
import wardrobe.project.com.wardrobeservice.service.CategoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Category management APIs")
@PreAuthorize("hasAuthority('ROLE_USER')")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/analytics/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get users who own items in a category (admin only)",
            description = "categoryName: one of 13 AI categories; granularity: day|month|year; date: YYYY-MM-DD")
    public ResponseEntity<ApiResponse<CategoryUsersResponseDTO>> getCategoryUsers(
            @RequestParam String categoryName,
            @RequestParam(required = false, defaultValue = "month") String granularity,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(ApiResponse.<CategoryUsersResponseDTO>builder()
                .success(true)
                .message("Category users fetched successfully")
                .data(categoryService.getCategoryUsers(categoryName, granularity, date))
                .build());
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get clothing item count per category (admin only)",
            description = "granularity: day|month|year, date: YYYY-MM-DD (defaults to today)")
    public ResponseEntity<ApiResponse<CategoryAnalyticsResponseDTO>> getCategoryAnalytics(
            @RequestParam(required = false, defaultValue = "month") String granularity,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(ApiResponse.<CategoryAnalyticsResponseDTO>builder()
                .success(true)
                .message("Category analytics fetched successfully")
                .data(categoryService.getCategoryAnalytics(granularity, date))
                .build());
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> createCategory(@Valid @RequestBody CategoryCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CategoryResponseDTO>builder()
                .success(true)
                .message("Category created successfully")
                .data(categoryService.createCategory(request))
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponseDTO>builder()
                .success(true)
                .message("Category fetched successfully")
                .data(categoryService.getCategoryById(id))
                .build());
    }

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponseDTO>>builder()
                .success(true)
                .message("All categories fetched successfully")
                .data(categoryService.getAllCategories())
                .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponseDTO>builder()
                .success(true)
                .message("Category updated successfully")
                .data(categoryService.updateCategory(id, request))
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Category deleted successfully")
                .data(null)
                .build());
    }
}
