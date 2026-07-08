package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.request.WardrobeZoneCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeZoneUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.WardrobeZoneResponseDTO;

import java.util.List;
import java.util.UUID;

public interface WardrobeZoneService {
    WardrobeZoneResponseDTO createZone(WardrobeZoneCreateRequestDTO request);
    WardrobeZoneResponseDTO getZoneById(UUID id);
    List<WardrobeZoneResponseDTO> getAllZones();
    List<WardrobeZoneResponseDTO> getZonesByWardrobeId(UUID wardrobeId);
    WardrobeZoneResponseDTO updateZone(UUID id, WardrobeZoneUpdateRequestDTO request);
    void deleteZone(UUID id);
    void restoreZone(UUID id);
    List<WardrobeZoneResponseDTO> searchZones(UUID wardrobeId, String keyword);
    List<WardrobeZoneResponseDTO> getDeletedZones(UUID userId);
}
