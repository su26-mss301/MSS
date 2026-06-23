package wardrobe.project.com.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import wardrobe.project.com.recommendationservice.dto.response.ApiResponse;
import wardrobe.project.com.recommendationservice.dto.response.OutfitResponseDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationResponseDTO;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;
import wardrobe.project.com.recommendationservice.dto.external.UserProfileExternalDTO;
import wardrobe.project.com.recommendationservice.engine.OutfitGenerator;
import wardrobe.project.com.recommendationservice.engine.RecommendationEngine;
import wardrobe.project.com.recommendationservice.entity.Event;
import wardrobe.project.com.recommendationservice.entity.Outfit;
import wardrobe.project.com.recommendationservice.entity.OutfitItem;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.repository.EventRepository;
import wardrobe.project.com.recommendationservice.repository.OutfitItemRepository;
import wardrobe.project.com.recommendationservice.repository.OutfitRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendItemRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl {

    private final RecommendationEngine engine;
    private final OutfitGenerator outfitGenerator;
    private final OutfitRepository outfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final RecommendItemRepository recommendItemRepository;
    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-service/users";
    private static final String WARDROBE_SERVICE_URL = "http://wardrobe-service/clothing-items";

    private UserProfileExternalDTO fetchUserProfile(UUID userId) {
        String url = USER_SERVICE_URL + "/" + userId + "/profile";
        try {
            return restTemplate.getForObject(url, UserProfileExternalDTO.class);
        } catch (Exception e) {
            log.error("Lỗi khi gọi User Service: ", e);
            throw new RuntimeException("Không thể lấy thông tin User Profile.");
        }
    }

    private List<ClothingItemExternalDTO> fetchUserWardrobe(UUID userId) {
        String url = WARDROBE_SERVICE_URL + "/user/" + userId;
        try {
            ResponseEntity<ApiResponse<List<ClothingItemExternalDTO>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<ApiResponse<List<ClothingItemExternalDTO>>>() {}
            );
            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Lỗi khi gọi Wardrobe Service: ", e);
            throw new RuntimeException("Không thể kết nối danh mục tủ đồ thật.");
        }
    }

    @Transactional(readOnly = true)
    public RecommendationResponseDTO getById(UUID id) {
        RecommendItem item = recommendItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi gợi ý với ID: " + id));
        return mapToResponse(item);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponseDTO> getAllByUserId(UUID userId) {
        List<RecommendItem> items = recommendItemRepository.findAll().stream()
                .filter(i -> i.getUserId().equals(userId))
                .collect(Collectors.toList());
        return items.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public RecommendationResponseDTO generateContentBased(UUID userId) {
        UserProfileExternalDTO profile = fetchUserProfile(userId);
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);

        List<ClothingItemExternalDTO> rankedItems = engine.rankByContentBased(wardrobe, profile);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        RecommendItem entity = saveRecommendation(userId, finalOutfit, null, 8.5f,
                "Phong Cách Cá Nhân", "Trang phục dựa trên sở thích cá nhân của bạn.");
        return mapToResponse(entity);
    }

    @Transactional
    public RecommendationResponseDTO generateEventBased(UUID userId, String eventType) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);

        List<ClothingItemExternalDTO> filteredItems = engine.filterByEvent(wardrobe, eventType);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(filteredItems);
        Event event = eventRepository.findByEventType(eventType).orElse(null);

        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, 9.2f,
                "Sự Kiện " + eventType, "Lựa chọn tối ưu dành cho dịp " + eventType);
        return mapToResponse(entity);
    }

    @Transactional
    public RecommendationResponseDTO generateCollaborative(UUID userId, UUID groupId) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        List<String> trendingStyles = List.of("Formal", "Casual");

        List<ClothingItemExternalDTO> rankedItems = engine.rankByCollaborative(wardrobe, trendingStyles);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        RecommendItem entity = saveRecommendation(userId, finalOutfit, null, 7.8f,
                "Xu Hướng Nhóm", "Gợi ý thịnh hành từ các thành viên trong nhóm bạn.");
        return mapToResponse(entity);
    }

    private RecommendItem saveRecommendation(UUID userId, List<ClothingItemExternalDTO> items, Event event,
                                             float score, String name, String description) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Không đủ quần áo phù hợp! Vui lòng thêm đồ vào tủ.");
        }

        Outfit outfit = new Outfit();
        outfit.setOutfitName(name);
        outfit.setDescription(description);
        outfit = outfitRepository.save(outfit);

        // ĐÃ SỬA: Lấy ID bằng getItemId() cho khớp với thuộc tính trong ClothingItemExternalDTO
        for (ClothingItemExternalDTO itemDto : items) {
            OutfitItem outfitItem = new OutfitItem();
            outfitItem.setOutfit(outfit);
            outfitItem.setItemId(itemDto.getItemId());
            outfitItemRepository.save(outfitItem);
        }

        RecommendItem rec = new RecommendItem();
        rec.setUserId(userId);
        rec.setOutfit(outfit);
        rec.setEvent(event);
        rec.setRecommendationScore(score);

        return recommendItemRepository.save(rec);
    }

    private RecommendationResponseDTO mapToResponse(RecommendItem item) {
        List<OutfitItem> outfitItems = outfitItemRepository.findByOutfit(item.getOutfit());
        int realItemCount = (outfitItems != null) ? outfitItems.size() : 0;

        List<String> realTags = new ArrayList<>();
        if (item.getEvent() != null && item.getEvent().getEventType() != null) {
            realTags.add(item.getEvent().getEventType());
        } else {
            realTags.add("Cá nhân hóa");
        }

        List<String> realSources = new ArrayList<>();
        if (item.getEvent() != null) {
            realSources.add("event");
        } else if (item.getOutfit().getOutfitName().toLowerCase().contains("nhóm")) {
            realSources.add("friendGroup");
        } else {
            realSources.add("preferences");
        }

        OutfitResponseDTO outfitDTO = OutfitResponseDTO.builder()
                .outfitId(item.getOutfit().getId())
                .outfitName(item.getOutfit().getOutfitName())
                .description(item.getOutfit().getDescription())
                .img(null)
                .items(realItemCount)
                .tags(realTags)
                .build();

        return RecommendationResponseDTO.builder()
                .recommendationId(item.getId())
                .userId(item.getUserId())
                .outfit(outfitDTO)
                .recommendationScore(item.getRecommendationScore())
                .eventType(item.getEvent() != null ? item.getEvent().getEventType() : "General")
                .sources(realSources)
                .build();
    }
}