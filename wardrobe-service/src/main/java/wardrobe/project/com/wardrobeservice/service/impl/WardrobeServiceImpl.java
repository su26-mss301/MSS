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
import wardrobe.project.com.wardrobeservice.service.WardrobeService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WardrobeServiceImpl implements WardrobeService {

    private final WardrobeRepository wardrobeRepository;

    @Override
    public WardrobeResponseDTO createWardrobe(WardrobeCreateRequestDTO request) {
        Wardrobe wardrobe = Wardrobe.builder()
                .userId(request.getUserId())
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
    public void deleteWardrobe(UUID id) {
        if (!wardrobeRepository.existsById(id)) {
            throw new AppException(ErrorCode.WARDROBE_NOT_FOUND);
        }
        wardrobeRepository.deleteById(id);
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
