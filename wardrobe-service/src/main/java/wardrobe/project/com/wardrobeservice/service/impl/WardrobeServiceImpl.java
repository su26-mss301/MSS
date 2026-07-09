package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.WardrobeResponseDTO;
import wardrobe.project.com.wardrobeservice.entity.Wardrobe;
import wardrobe.project.com.wardrobeservice.exception.AppException;
import wardrobe.project.com.wardrobeservice.exception.ErrorCode;
import wardrobe.project.com.wardrobeservice.repository.WardrobeRepository;
import wardrobe.project.com.wardrobeservice.repository.WardrobeZoneRepository;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.service.WardrobeService;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WardrobeServiceImpl implements WardrobeService {

    private final WardrobeRepository wardrobeRepository;
    private final WardrobeZoneRepository wardrobeZoneRepository;
    private final ClothingItemRepository clothingItemRepository;

    @Override
    public WardrobeResponseDTO createWardrobe(UUID userId, WardrobeCreateRequestDTO request) {
        Wardrobe wardrobe = Wardrobe.builder()
                .userId(userId)
                .wardrobeName(request.getWardrobeName())
                .build();
        Wardrobe savedWardrobe = wardrobeRepository.save(wardrobe);
        return mapToResponse(savedWardrobe);
    }

    @Override
    public WardrobeResponseDTO getWardrobeById(UUID id) {
        Wardrobe wardrobe = wardrobeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_NOT_FOUND));
        return mapToResponse(wardrobe);
    }

    @Override
    public List<WardrobeResponseDTO> getAllWardrobes() {
        return wardrobeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardrobeResponseDTO> getWardrobesByUserId(UUID userId) {
        return wardrobeRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WardrobeResponseDTO updateWardrobe(UUID id, WardrobeUpdateRequestDTO request) {
        Wardrobe wardrobe = wardrobeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_NOT_FOUND));
        
        wardrobe.setWardrobeName(request.getWardrobeName());
        // update other fields if necessary
        
        Wardrobe updatedWardrobe = wardrobeRepository.save(wardrobe);
        return mapToResponse(updatedWardrobe);
    }

    @Override
    @Transactional
    public void deleteWardrobe(UUID id) {
        Wardrobe wardrobe = wardrobeRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_NOT_FOUND));
            
        LocalDateTime now = LocalDateTime.now();
        wardrobe.setDeletedAt(now);
        if (wardrobe.getZones() != null) {
            wardrobe.getZones().forEach(zone -> {
                zone.setDeletedAt(now);
                if (zone.getItems() != null) {
                    zone.getItems().forEach(item -> item.setDeletedAt(now));
                }
            });
        }
        wardrobeRepository.save(wardrobe);
    }

    @Override
    @Transactional
    public void restoreWardrobe(UUID id) {
        wardrobeRepository.restoreWardrobe(id);
        wardrobeZoneRepository.restoreZonesByWardrobeId(id);
        clothingItemRepository.restoreItemsByWardrobeId(id);
    }

    @Override
    public List<WardrobeResponseDTO> searchWardrobes(UUID userId, String keyword) {
        List<Wardrobe> wardrobes = wardrobeRepository.findByUserIdAndWardrobeNameContainingIgnoreCase(userId, keyword);
        if (wardrobes.isEmpty()) {
            throw new AppException(ErrorCode.WARDROBE_NOT_FOUND);
        }
        return wardrobes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardrobeResponseDTO> getDeletedWardrobes(UUID userId) {
        return wardrobeRepository.findDeletedByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WardrobeResponseDTO mapToResponse(Wardrobe wardrobe) {
        return WardrobeResponseDTO.builder()
                .wardrobeId(wardrobe.getWardrobeId())
                .userId(wardrobe.getUserId())
                .wardrobeName(wardrobe.getWardrobeName())
                .createdAt(wardrobe.getCreatedAt())
                .build();
    }
}
