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
import java.util.Objects;
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

    private float calculateRealScore(List<ClothingItemExternalDTO> outfit) {
        if (outfit == null || outfit.isEmpty()) return 0f;
        double avgScore = outfit.stream()
                .mapToDouble(item -> item.getConfidenceScore() != null ? item.getConfidenceScore() : 0.85)
                .average()
                .orElse(0.85);
        return (float) (avgScore * 10);
    }

    private String generateDynamicName(List<ClothingItemExternalDTO> outfit, String context) {
        if (outfit == null || outfit.isEmpty()) return "Trang Phục " + context;
        String mainStyle = outfit.stream()
                .map(ClothingItemExternalDTO::getStyle)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Đa Phong Cách");
        return "Set Đồ " + mainStyle + " (" + context + ")";
    }

    private String generateDynamicDescription(List<ClothingItemExternalDTO> outfit) {
        if (outfit == null || outfit.isEmpty()) return "Gợi ý tự động từ hệ thống AI.";
        String itemDetails = outfit.stream()
                .map(item -> item.getItemName() + " màu " + item.getDominantColor())
                .collect(Collectors.joining(", "));
        return "Bộ trang phục được phối từ các vật phẩm thực tế trong tủ của bạn bao gồm: " + itemDetails + ".";
    }

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
        try {
            String url = "http://user-service/api/v1/users/me";
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, createForwardingHeaders(), JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                UserProfileExternalDTO profile = new UserProfileExternalDTO();
                profile.setId(userId);
                return profile;
            }
        } catch (Exception e) {
            log.warn("Lấy Profile thất bại: {}", e.getMessage());
        }
        UserProfileExternalDTO fallbackProfile = new UserProfileExternalDTO();
        fallbackProfile.setId(userId);
        return fallbackProfile;
    }

    private List<ClothingItemExternalDTO> fetchUserWardrobe(UUID userId) {
        List<ClothingItemExternalDTO> allItems = new ArrayList<>();
        try {
            HttpEntity<String> entity = createForwardingHeaders();
            String wardrobeUrl = "http://wardrobe-service/api/v1/wardrobe/wardrobes/user/" + userId;
            ResponseEntity<JsonNode> wardrobeRes = restTemplate.exchange(wardrobeUrl, HttpMethod.GET, entity, JsonNode.class);
            JsonNode wBody = wardrobeRes.getBody();

            if (wBody != null && wBody.hasNonNull("data")) {
                for (JsonNode wNode : wBody.path("data")) {
                    String wardrobeId = wNode.path("wardrobeId").asText();
                    String zoneUrl = "http://wardrobe-service/api/v1/wardrobe/wardrobe-zones/wardrobe/" + wardrobeId;
                    ResponseEntity<JsonNode> zoneRes = restTemplate.exchange(zoneUrl, HttpMethod.GET, entity, JsonNode.class);
                    JsonNode zBody = zoneRes.getBody();

                    if (zBody != null && zBody.hasNonNull("data")) {
                        for (JsonNode zNode : zBody.path("data")) {
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
            log.error("LỖI GỌI WARDROBE SERVICE: ", e);
            return new ArrayList<>();
        }
    }

    private Event getOrCreateEvent(String eventType) {
        return eventRepository.findByEventType(eventType).orElseGet(() -> {
            Event newEvent = new Event();
            newEvent.setEventType(eventType);
            newEvent.setEventName(eventType);
            return eventRepository.save(newEvent);
        });
    }

    @Transactional(readOnly = true)
    public RecommendationResponseDTO getById(UUID id) {
        RecommendItem item = recommendItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gợi ý với ID: " + id));
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(item.getUserId());
        return mapToResponse(item, wardrobe);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponseDTO> getAllByUserId(UUID userId) {
        List<RecommendItem> items = recommendItemRepository.findAll().stream()
                .filter(i -> i.getUserId().equals(userId)).collect(Collectors.toList());
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        return items.stream().map(i -> mapToResponse(i, wardrobe)).collect(Collectors.toList());
    }

    @Transactional
    public RecommendationResponseDTO generateContentBased(UUID userId) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) throw new RuntimeException("Không tìm thấy quần áo nào trong tủ!");

        UserProfileExternalDTO profile = fetchUserProfile(userId);
        List<ClothingItemExternalDTO> rankedItems = engine.rankByContentBased(wardrobe, profile);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        float realScore = calculateRealScore(finalOutfit);
        String realName = generateDynamicName(finalOutfit, "Cá Nhân");
        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent("Casual");
        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, realScore, realName, realDesc);
        return mapToResponse(entity, wardrobe);
    }

    @Transactional
    public RecommendationResponseDTO generateEventBased(UUID userId, String eventType) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) throw new RuntimeException("Không tìm thấy quần áo nào trong tủ!");

        List<ClothingItemExternalDTO> filteredItems = engine.filterByEvent(wardrobe, eventType);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(filteredItems);

        float realScore = calculateRealScore(finalOutfit);
        String realName = generateDynamicName(finalOutfit, eventType);
        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent(eventType);
        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, realScore, realName, realDesc);
        return mapToResponse(entity, wardrobe);
    }

    @Transactional
    public RecommendationResponseDTO generateCollaborative(UUID userId, UUID groupId) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) throw new RuntimeException("Không tìm thấy quần áo nào trong tủ!");

        List<String> trendingStyles = List.of("Formal", "Casual");
        List<ClothingItemExternalDTO> rankedItems = engine.rankByCollaborative(wardrobe, trendingStyles);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        float realScore = calculateRealScore(finalOutfit);
        String realName = generateDynamicName(finalOutfit, "Nhóm Bạn");
        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent("Party");
        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, realScore, realName, realDesc);
        return mapToResponse(entity, wardrobe);
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

    private RecommendationResponseDTO mapToResponse(RecommendItem item, List<ClothingItemExternalDTO> availableItems) {
        List<OutfitItem> outfitItems = outfitItemRepository.findByOutfit(item.getOutfit());
        List<ClothingItemExternalDTO> realClothingDetails = new ArrayList<>();
        if (outfitItems != null && availableItems != null) {
            for (OutfitItem oi : outfitItems) {
                availableItems.stream()
                        .filter(c -> c.getItemId().equals(oi.getItemId()))
                        .findFirst()
                        .ifPresent(realClothingDetails::add);
            }
        }

        List<String> realTags = new ArrayList<>();
        if (item.getEvent() != null && item.getEvent().getEventType() != null) {
            realTags.add(item.getEvent().getEventType());
        }

        List<String> realSources = new ArrayList<>();
        if (item.getEvent() != null) realSources.add("event");
        else realSources.add("preferences");

        OutfitResponseDTO outfitDTO = OutfitResponseDTO.builder()
                .outfitId(item.getOutfit().getId())
                .outfitName(item.getOutfit().getOutfitName())
                .description(item.getOutfit().getDescription())
                .img(null)
                .items(realClothingDetails.size())
                .tags(realTags)
                .clothingItems(realClothingDetails)
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