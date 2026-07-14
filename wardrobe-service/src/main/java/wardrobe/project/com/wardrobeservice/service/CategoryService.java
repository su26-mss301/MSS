package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.request.CategoryCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.CategoryUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryAnalyticsResponseDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryResponseDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryUsersResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponseDTO createCategory(CategoryCreateRequestDTO request);
    CategoryResponseDTO getCategoryById(UUID id);
    List<CategoryResponseDTO> getAllCategories();
    CategoryResponseDTO updateCategory(UUID id, CategoryUpdateRequestDTO request);
    void deleteCategory(UUID id);

    /**
     * Thống kê số clothing_item thêm vào theo 13 danh mục,
     * lọc theo granularity (day|month|year) và date (YYYY-MM-DD).
     */
    CategoryAnalyticsResponseDTO getCategoryAnalytics(String granularity, String date);

    /**
     * Danh sách user có trang phục thuộc danh mục, kèm số lượng mỗi user.
     */
    CategoryUsersResponseDTO getCategoryUsers(String categoryName, String granularity, String date);
}
