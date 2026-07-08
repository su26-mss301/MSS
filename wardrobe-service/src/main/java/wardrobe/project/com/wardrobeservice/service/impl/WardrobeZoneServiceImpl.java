package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeZoneCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeZoneUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.WardrobeZoneResponseDTO;
import wardrobe.project.com.wardrobeservice.entity.Wardrobe;
import wardrobe.project.com.wardrobeservice.entity.WardrobeZone;
import wardrobe.project.com.wardrobeservice.exception.AppException;
import wardrobe.project.com.wardrobeservice.exception.ErrorCode;
import wardrobe.project.com.wardrobeservice.repository.WardrobeRepository;
import wardrobe.project.com.wardrobeservice.repository.WardrobeZoneRepository;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.service.WardrobeZoneService;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WardrobeZoneServiceImpl implements WardrobeZoneService {

    private final WardrobeZoneRepository wardrobeZoneRepository;
    private final WardrobeRepository wardrobeRepository;
    private final ClothingItemRepository clothingItemRepository;

    @Override
    public WardrobeZoneResponseDTO createZone(WardrobeZoneCreateRequestDTO request) {
        Wardrobe wardrobe = wardrobeRepository.findById(request.getWardrobeId())
                .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_NOT_FOUND));

        WardrobeZone zone = WardrobeZone.builder()
                .wardrobe(wardrobe)
                .zoneName(request.getZoneName())
                .description(request.getDescription())
                .build();
        WardrobeZone savedZone = wardrobeZoneRepository.save(zone);
        return mapToResponse(savedZone);
    }

    @Override
    public WardrobeZoneResponseDTO getZoneById(UUID id) {
        WardrobeZone zone = wardrobeZoneRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));
        return mapToResponse(zone);
    }

    @Override
    public List<WardrobeZoneResponseDTO> getAllZones() {
        return wardrobeZoneRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardrobeZoneResponseDTO> getZonesByWardrobeId(UUID wardrobeId) {
        return wardrobeZoneRepository.findByWardrobe_WardrobeId(wardrobeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WardrobeZoneResponseDTO updateZone(UUID id, WardrobeZoneUpdateRequestDTO request) {
        WardrobeZone zone = wardrobeZoneRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));

        if (!zone.getWardrobe().getWardrobeId().equals(request.getWardrobeId())) {
            Wardrobe wardrobe = wardrobeRepository.findById(request.getWardrobeId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_NOT_FOUND));
            zone.setWardrobe(wardrobe);
        }

        zone.setZoneName(request.getZoneName());
        zone.setDescription(request.getDescription());

        WardrobeZone updatedZone = wardrobeZoneRepository.save(zone);
        return mapToResponse(updatedZone);
    }

    @Override
    @Transactional
    public void deleteZone(UUID id) {
        WardrobeZone zone = wardrobeZoneRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));
            
        LocalDateTime now = LocalDateTime.now();
        zone.setDeletedAt(now);
        if (zone.getItems() != null) {
            zone.getItems().forEach(item -> item.setDeletedAt(now));
        }
        wardrobeZoneRepository.save(zone);
    }

    @Override
    @Transactional
    public void restoreZone(UUID id) {
        Integer isParentDeleted = wardrobeZoneRepository.isParentWardrobeDeleted(id);
        if (isParentDeleted != null && isParentDeleted == 1) {
            throw new AppException(ErrorCode.PARENT_WARDROBE_DELETED);
        }

        wardrobeZoneRepository.restoreZone(id);
        clothingItemRepository.restoreItemsByZoneId(id);
    }

    @Override
    public List<WardrobeZoneResponseDTO> searchZones(UUID wardrobeId, String keyword) {
        List<WardrobeZone> zones;
        if (wardrobeId != null) {
            zones = wardrobeZoneRepository.findByWardrobe_WardrobeIdAndZoneNameContainingIgnoreCase(wardrobeId, keyword);
        } else {
            zones = wardrobeZoneRepository.findByZoneNameContainingIgnoreCase(keyword);
        }
        
        if (zones.isEmpty()) {
            throw new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND);
        }
        
        return zones.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WardrobeZoneResponseDTO> getDeletedZones(UUID userId) {
        return wardrobeZoneRepository.findDeletedByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WardrobeZoneResponseDTO mapToResponse(WardrobeZone zone) {
        return WardrobeZoneResponseDTO.builder()
                .zoneId(zone.getZoneId())
                .wardrobeId(zone.getWardrobe().getWardrobeId())
                .zoneName(zone.getZoneName())
                .description(zone.getDescription())
                .build();
    }
}
