package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ClothingItemResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ClothingItemService {
    ClothingItemResponseDTO createClothingItem(ClothingItemCreateRequestDTO request);
    ClothingItemResponseDTO getClothingItemById(UUID id);
    List<ClothingItemResponseDTO> getAllClothingItems();
    List<ClothingItemResponseDTO> getItemsByZoneId(UUID zoneId);
    List<ClothingItemResponseDTO> getItemsByCategoryId(UUID categoryId);
    ClothingItemResponseDTO updateClothingItem(UUID id, ClothingItemUpdateRequestDTO request);
    void deleteClothingItem(UUID id);
}
