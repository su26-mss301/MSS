package wardrobe.project.com.recommendationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    private static final String USER_SERVICE_URL = "http://user-service/api/v1/users";
    private static final String WARDROBE_API_BASE = "http://wardrobe-service/api/v1/wardrobe";

    private HttpEntity<String> createForwardingHeaders() {
        HttpHeaders headers = new HttpHeaders();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader("Authorization");
                if (token != null) headers.set("Authorization", token);
                String actorType = request.getHeader("X-Auth-Actor-Type");
                if (actorType != null) headers.set("X-Auth-Actor-Type", actorType);
                String userId = request.getHeader("X-Auth-User-Id");
                if (userId != null) headers.set("X-Auth-User-Id", userId);
            }
        } catch (Exception e) {
            log.warn("Không lấy được header", e);
        }
        return new HttpEntity<>(headers);
    }

    private UserProfileExternalDTO fetchUserProfile(UUID userId) {
        String url = USER_SERVICE_URL + "/me";
        try {
            restTemplate.exchange(url, HttpMethod.GET, createForwardingHeaders(), JsonNode.class);
        } catch (Exception e) {
            log.warn("Cảnh báo khi xác thực user qua /me: {}", e.getMessage());
        }

        UserProfileExternalDTO fallbackProfile = new UserProfileExternalDTO();
        fallbackProfile.setId(userId);
        fallbackProfile.setFavoriteColors(new ArrayList<>());
        fallbackProfile.setPreferredStyle("");
        return fallbackProfile;
    }

    private List<ClothingItemExternalDTO> fetchUserWardrobe(UUID userId) {
        List<ClothingItemExternalDTO> allItems = new ArrayList<>();
        try {
            HttpEntity<String> entity = createForwardingHeaders();

            String wardrobeUrl = "http://wardrobe-service/api/v1/wardrobe/wardrobes/user/" + userId;

            ResponseEntity<ApiResponse<List<JsonNode>>> wardrobeRes = restTemplate.exchange(
                    wardrobeUrl, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<ApiResponse<List<JsonNode>>>() {}
            );

            if (wardrobeRes.getBody() != null && wardrobeRes.getBody().getData() != null) {
                for (JsonNode wNode : wardrobeRes.getBody().getData()) {
                    String wardrobeId = wNode.path("wardrobeId").asText();

                    String zoneUrl = "http://wardrobe-service/api/v1/wardrobe/wardrobe-zones/wardrobe/" + wardrobeId;
                    ResponseEntity<ApiResponse<List<JsonNode>>> zoneRes = restTemplate.exchange(
                            zoneUrl, HttpMethod.GET, entity,
                            new ParameterizedTypeReference<ApiResponse<List<JsonNode>>>() {}
                    );

                    if (zoneRes.getBody() != null && zoneRes.getBody().getData() != null) {
                        for (JsonNode zNode : zoneRes.getBody().getData()) {
                            String zoneId = zNode.path("zoneId").asText();

                            String itemUrl = "http://wardrobe-service/api/v1/wardrobe/clothing-items/zone/" + zoneId;
                            ResponseEntity<ApiResponse<List<ClothingItemExternalDTO>>> itemRes = restTemplate.exchange(
                                    itemUrl, HttpMethod.GET, entity,
                                    new ParameterizedTypeReference<ApiResponse<List<ClothingItemExternalDTO>>>() {}
                            );

                            if (itemRes.getBody() != null && itemRes.getBody().getData() != null) {
                                allItems.addAll(itemRes.getBody().getData());
                            }
                        }
                    }
                }
            }
            return allItems;
        } catch (Exception e) {
            log.error("Lỗi lấy dữ liệu từ wardrobe-service: ", e);
            throw new RuntimeException("Không thể lấy dữ liệu từ Wardrobe Service"); // Ép buộc báo lỗi nếu gọi service thất bại
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
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);

        if (wardrobe.isEmpty()) {
            log.error("Wardrobe trả về rỗng cho user {}", userId);
            throw new RuntimeException("Không tìm thấy quần áo nào trong tủ của bạn!");
        }

        UserProfileExternalDTO profile = new UserProfileExternalDTO(); // Fallback
        List<ClothingItemExternalDTO> rankedItems = engine.rankByContentBased(wardrobe, profile);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        RecommendItem entity = saveRecommendation(userId, finalOutfit, null, 8.5f, "Cá Nhân", "...");
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

    private RecommendItem saveRecommendation(UUID userId, List<ClothingItemExternalDTO> items, Event event, float score, String name, String description) {
        Outfit outfit = new Outfit();
        outfit.setOutfitName(name);
        outfit.setDescription(description);
        outfit = outfitRepository.save(outfit);
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