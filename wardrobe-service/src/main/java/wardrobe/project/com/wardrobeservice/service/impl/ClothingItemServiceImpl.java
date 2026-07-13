package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ClothingItemResponseDTO;
import wardrobe.project.com.wardrobeservice.entity.Category;
import wardrobe.project.com.wardrobeservice.entity.ClothingItem;
import wardrobe.project.com.wardrobeservice.entity.WardrobeZone;
import wardrobe.project.com.wardrobeservice.exception.AppException;
import wardrobe.project.com.wardrobeservice.exception.ErrorCode;
import wardrobe.project.com.wardrobeservice.repository.CategoryRepository;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.repository.WardrobeZoneRepository;
import wardrobe.project.com.wardrobeservice.service.ClothingItemService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClothingItemServiceImpl implements ClothingItemService {

    private final ClothingItemRepository clothingItemRepository;
    private final WardrobeZoneRepository wardrobeZoneRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public ClothingItemResponseDTO createClothingItem(ClothingItemCreateRequestDTO request) {
        WardrobeZone zone = null;
        if (request.getZoneId() != null) {
            zone = wardrobeZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        ClothingItem item = ClothingItem.builder()
                .zone(zone)
                .category(category)
                .imageId(request.getImageId())
                .itemName(request.getItemName())
                .dominantColor(request.getDominantColor())
                .style(request.getStyle())
                .confidenceScore(request.getConfidenceScore())
                .build();
        
        ClothingItem savedItem = clothingItemRepository.save(item);
        return mapToResponse(savedItem);
    }

    @Override
    public ClothingItemResponseDTO getClothingItemById(UUID id) {
        ClothingItem item = clothingItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND));
        return mapToResponse(item);
    }

    @Override
    public List<ClothingItemResponseDTO> getAllClothingItems(UUID userId) {
        return clothingItemRepository.findByZone_Wardrobe_UserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClothingItemResponseDTO> getItemsByZoneId(UUID zoneId) {
        return clothingItemRepository.findByZone_ZoneId(zoneId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClothingItemResponseDTO> getItemsByCategoryId(UUID categoryId) {
        return clothingItemRepository.findByCategory_CategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClothingItemResponseDTO updateClothingItem(UUID id, ClothingItemUpdateRequestDTO request) {
        ClothingItem item = clothingItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND));

        if (request.getZoneId() != null) {
            WardrobeZone zone = wardrobeZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));
            item.setZone(zone);
        } else {
            item.setZone(null);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            item.setCategory(category);
        } else {
            item.setCategory(null);
        }

        item.setImageId(request.getImageId());
        item.setItemName(request.getItemName());
        item.setDominantColor(request.getDominantColor());
        item.setStyle(request.getStyle());
        item.setConfidenceScore(request.getConfidenceScore());

        ClothingItem updatedItem = clothingItemRepository.save(item);
        return mapToResponse(updatedItem);
    }

    @Override
    public void deleteClothingItem(UUID id) {
        if (!clothingItemRepository.existsById(id)) {
            throw new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND);
        }
        clothingItemRepository.deleteById(id);
    }

    private ClothingItemResponseDTO mapToResponse(ClothingItem item) {
        return ClothingItemResponseDTO.builder()
                .itemId(item.getItemId())
                .zoneId(item.getZone() != null ? item.getZone().getZoneId() : null)
                .categoryId(item.getCategory() != null ? item.getCategory().getCategoryId() : null)
                .imageId(item.getImageId())
                .itemName(item.getItemName())
                .dominantColor(item.getDominantColor())
                .style(item.getStyle())
                .confidenceScore(item.getConfidenceScore())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
