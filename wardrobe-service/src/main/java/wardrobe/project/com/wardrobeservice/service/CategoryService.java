package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.request.CategoryCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.CategoryUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponseDTO createCategory(CategoryCreateRequestDTO request);
    CategoryResponseDTO getCategoryById(UUID id);
    List<CategoryResponseDTO> getAllCategories();
    CategoryResponseDTO updateCategory(UUID id, CategoryUpdateRequestDTO request);
    void deleteCategory(UUID id);
}
