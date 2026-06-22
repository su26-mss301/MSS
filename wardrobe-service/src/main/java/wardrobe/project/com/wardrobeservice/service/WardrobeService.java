package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.request.WardrobeCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.WardrobeUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.WardrobeResponseDTO;

import java.util.List;
import java.util.UUID;

public interface WardrobeService {
    WardrobeResponseDTO createWardrobe(UUID userId, WardrobeCreateRequestDTO request);
    WardrobeResponseDTO getWardrobeById(UUID id);
    List<WardrobeResponseDTO> getAllWardrobes();
    List<WardrobeResponseDTO> getWardrobesByUserId(UUID userId);
    WardrobeResponseDTO updateWardrobe(UUID id, WardrobeUpdateRequestDTO request);
    void deleteWardrobe(UUID id);
    List<WardrobeResponseDTO> searchWardrobes(String keyword);
}
