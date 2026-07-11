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

import java.util.*;
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

    private float calculateRealScore(List<ClothingItemExternalDTO> outfit) {
        if (outfit == null || outfit.isEmpty()) return 0f;
        double avgScore = outfit.stream()
                .mapToDouble(item -> item.getConfidenceScore() != null ? item.getConfidenceScore() : 0.85)
                .average()
                .orElse(0.85);
        return (float) (avgScore * 10);
    }

    private String getFriendlyVietnameseName(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return "Đa Phong Cách";

        String primaryWord = rawText.split(",")[0].trim().toLowerCase();

        return switch (primaryWord) {
            case "minimal" -> "Tối Giản";
            case "casual" -> "Thường Ngày";
            case "office", "meeting" -> "Công Sở";
            case "elegant" -> "Thanh Lịch";
            case "street" -> "Đường Phố";
            case "sporty" -> "Thể Thao";
            case "bohemian" -> "Boho";
            case "vintage" -> "Cổ Điển";
            case "party" -> "Tiệc Tùng";
            case "wedding" -> "Dự Tiệc";
            case "travel" -> "Du Lịch";
            case "interview" -> "Phỏng Vấn";
            default -> "Thời Trang";
        };
    }

    private String generateDynamicName(List<ClothingItemExternalDTO> outfit, String context, String targetStyle) {
        if (outfit == null || outfit.isEmpty()) {
            return "Trang Phục Tự Động";
        }

        if ("Cá Nhân".equalsIgnoreCase(context)) {
            String friendlyStyle = getFriendlyVietnameseName(targetStyle);
            return "Set Đồ " + friendlyStyle + " (Cá Nhân)";
        }
        else if ("Nhóm Bạn".equalsIgnoreCase(context)) {
            String friendlyStyle = getFriendlyVietnameseName(targetStyle);
            return "Xu Hướng " + friendlyStyle + " (Nhóm Bạn)";
        }
        else {
            String friendlyEvent = getFriendlyVietnameseName(context);
            return "Trang Phục " + friendlyEvent;
        }
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

                // Tự động chuyển tiếp toàn bộ header X-Auth-* (đã được common-auth xử lý)
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames != null && headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (headerName.toLowerCase().startsWith("x-auth-")) {
                        headers.set(headerName, request.getHeader(headerName));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi lấy header nội bộ: ", e);
        }
        return new HttpEntity<>(headers);
    }

    private UserProfileExternalDTO fetchUserProfile(UUID userId) {
        UserProfileExternalDTO profile = new UserProfileExternalDTO();
        profile.setId(userId);
        try {
            RestTemplate directRestTemplate = new RestTemplate();
            String url = "http://localhost:8081/api/v1/users/style-preferences/me";
            ResponseEntity<JsonNode> response = directRestTemplate.exchange(url, HttpMethod.GET, createForwardingHeaders(), JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode dataNode = response.getBody().has("data") ? response.getBody().path("data") : response.getBody();

                if (dataNode.hasNonNull("preferredStyles") && dataNode.path("preferredStyles").isArray()) {
                    List<String> styles = new ArrayList<>();
                    for (JsonNode styleNode : dataNode.path("preferredStyles")) {
                        styles.add(styleNode.asText());
                    }
                    if (!styles.isEmpty()) {
                        profile.setPreferredStyle(String.join(",", styles));
                    }
                }

                if (dataNode.hasNonNull("favoriteColors") && dataNode.path("favoriteColors").isArray()) {
                    List<String> colors = new ArrayList<>();
                    for (JsonNode colorNode : dataNode.path("favoriteColors")) {
                        colors.add(colorNode.asText());
                    }
                    profile.setFavoriteColors(colors);
                }

                log.info("DEBUG: Khớp thành công toàn bộ sở thích phong cách: {}", profile.getPreferredStyle());
                return profile;
            }
        } catch (Exception e) {
            log.warn("Lấy Profile thất bại: {}", e.getMessage());
        }
        return profile;
    }

    private List<ClothingItemExternalDTO> fetchUserWardrobe(UUID userId) {
        List<ClothingItemExternalDTO> allItems = new ArrayList<>();
        try {
            HttpEntity<String> entity = createForwardingHeaders();
            RestTemplate directRestTemplate = new RestTemplate();

            // GỌI TRỰC TIẾP WARDROBE-SERVICE Ở CỔNG 8082
            String wardrobeUrl = "http://localhost:8082/api/v1/wardrobe/wardrobes";
            ResponseEntity<JsonNode> wardrobeRes = directRestTemplate.exchange(wardrobeUrl, HttpMethod.GET, entity, JsonNode.class);
            JsonNode wBody = wardrobeRes.getBody();

            if (wBody != null && wBody.hasNonNull("data")) {
                for (JsonNode wNode : wBody.path("data")) {
                    String wUserId = wNode.path("userId").asText();
                    if (userId != null && !userId.toString().equalsIgnoreCase(wUserId)) {
                        continue;
                    }

                    String wardrobeId = wNode.path("wardrobeId").asText();

                    String zoneUrl = "http://localhost:8082/api/v1/wardrobe/wardrobe-zones/wardrobe/" + wardrobeId;
                    ResponseEntity<JsonNode> zoneRes = directRestTemplate.exchange(zoneUrl, HttpMethod.GET, entity, JsonNode.class);
                    JsonNode zBody = zoneRes.getBody();

                    if (zBody != null && zBody.hasNonNull("data")) {
                        for (JsonNode zNode : zBody.path("data")) {
                            String zoneId = zNode.path("zoneId").asText();

                            String itemUrl = "http://localhost:8082/api/v1/wardrobe/clothing-items/zone/" + zoneId;
                            ResponseEntity<ApiResponse<List<ClothingItemExternalDTO>>> itemRes = directRestTemplate.exchange(
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
            log.error("LỖI KHI TRUY XUẤT DỮ LIỆU TỪ WARDROBE SERVICE: ", e);
            return new ArrayList<>();
        }
    }

    private Event getOrCreateEvent(String eventType) {
        return eventRepository.findByEventType(eventType).orElseGet(() -> {
            Event newEvent = new Event();
            newEvent.setEventType(eventType);
            newEvent.setEventName(newEvent.getEventType());
            return eventRepository.save(newEvent);
        });
    }

    private List<UUID> fetchGroupMemberIds(UUID userId, UUID groupId) {
        List<UUID> memberIds = new ArrayList<>();
        try {
            RestTemplate directRestTemplate = new RestTemplate();
            String url = "http://localhost:8081/api/v1/users/friend-groups/" + groupId + "/detail";
            ResponseEntity<JsonNode> response = directRestTemplate.exchange(url, HttpMethod.GET, createForwardingHeaders(), JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode membersNode = response.getBody().path("data").path("members");
                if (membersNode.isArray()) {
                    for (JsonNode m : membersNode) {
                        memberIds.add(UUID.fromString(m.path("userId").asText()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lấy danh sách thành viên nhóm thất bại: {}", e.getMessage());
        }
        return memberIds;
    }

    private static class GroupInfo {
        String groupName;
        List<String> styles;

        public GroupInfo(String groupName, List<String> styles) {
            this.groupName = groupName;
            this.styles = styles;
        }
    }

    private GroupInfo fetchGroupInfo(UUID userId, UUID groupId) {
        try {
            RestTemplate directRestTemplate = new RestTemplate();
            String url = "http://localhost:8081/api/v1/users/friend-groups/" + groupId + "/detail";
            ResponseEntity<JsonNode> response = directRestTemplate.exchange(url, HttpMethod.GET, createForwardingHeaders(), JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode dataNode = response.getBody().path("data");

                boolean isMember = false;
                JsonNode membersNode = dataNode.path("members");
                if (membersNode.isArray()) {
                    for (JsonNode m : membersNode) {
                        if (userId.toString().equalsIgnoreCase(m.path("userId").asText())) {
                            isMember = true;
                            break;
                        }
                    }
                }

                if (!isMember) return null;

                String groupName = dataNode.path("groupName").asText();
                if (groupName == null || groupName.isEmpty()) {
                    groupName = "Bạn Bè";
                }

                List<String> trendingStyles = new ArrayList<>();
                JsonNode stylesNode = dataNode.path("commonStyles");
                if (stylesNode != null && stylesNode.isArray()) {
                    for (JsonNode s : stylesNode) {
                        String styleName = s.path("styleName").asText();
                        if (styleName != null && !styleName.isEmpty()) {
                            trendingStyles.add(styleName.toLowerCase());
                        }
                    }
                }

                if (trendingStyles.isEmpty()) {
                    trendingStyles.addAll(List.of("casual", "minimal"));
                }
                return new GroupInfo(groupName, trendingStyles);
            }
        } catch (Exception e) {
            log.warn("Lấy chi tiết và thống kê nhóm thất bại: {}", e.getMessage());
        }
        return null;
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
        UserProfileExternalDTO profile = fetchUserProfile(userId);
        if (profile == null || profile.getPreferredStyle() == null || profile.getPreferredStyle().trim().isEmpty()) {
            log.warn("Người dùng {} chưa thiết lập phong cách cá nhân trong hồ sơ.", userId);

            OutfitResponseDTO noPreferenceOutfit = OutfitResponseDTO.builder()
                    .outfitName("Chưa thiết lập phong cách cá nhân")
                    .description("Hệ thống chưa thể đưa ra gợi ý cá nhân hóa do bạn chưa chọn phong cách ưa thích. Vui lòng truy cập mục Cài đặt sở thích trên giao diện để thiết lập gu thời trang và màu sắc của mình trước.")
                    .items(0)
                    .clothingItems(new ArrayList<>())
                    .build();

            return RecommendationResponseDTO.builder()
                    .userId(userId)
                    .outfit(noPreferenceOutfit)
                    .recommendationScore(0f)
                    .eventType("Cá Nhân")
                    .build();
        }

        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) {
            log.warn("Hệ thống phát hiện tủ đồ trống đối với người dùng: {}", userId);
            return createEmptyRecommendationResponse(userId, "Cá Nhân");
        }

        List<ClothingItemExternalDTO> rankedItems = engine.rankByContentBased(wardrobe, profile);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        float realScore = calculateRealScore(finalOutfit);
        String realName = generateDynamicName(finalOutfit, "Cá Nhân", profile.getPreferredStyle());
        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent("Casual");
        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, realScore, realName, realDesc);
        return mapToResponse(entity, wardrobe);
    }

    @Transactional
    public RecommendationResponseDTO generateEventBased(UUID userId, String eventType) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) {
            log.warn("Hệ thống phát hiện tủ đồ trống đối với người dùng: {}", userId);
            return createEmptyRecommendationResponse(userId, eventType);
        }

        List<ClothingItemExternalDTO> filteredItems = engine.filterByEvent(wardrobe, eventType);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(filteredItems);

        float realScore = calculateRealScore(finalOutfit);

        UserProfileExternalDTO profile = fetchUserProfile(userId);
        String fallbackStyle = (profile.getPreferredStyle() != null) ? profile.getPreferredStyle() : eventType;
        String realName = generateDynamicName(finalOutfit, eventType, fallbackStyle);

        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent(eventType);
        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, realScore, realName, realDesc);
        return mapToResponse(entity, wardrobe);
    }

    @Transactional
    public RecommendationResponseDTO generateCollaborative(UUID userId, UUID groupId) {
        GroupInfo groupInfo = fetchGroupInfo(userId, groupId);

        if (groupInfo == null) {
            log.warn("Người dùng {} chưa tham gia vào nhóm bạn {} (Hoặc GroupId không hợp lệ).", userId, groupId);

            OutfitResponseDTO noGroupOutfit = OutfitResponseDTO.builder()
                    .outfitName("Chưa tham gia nhóm bạn nào")
                    .description("Hệ thống chưa thể đưa ra gợi ý theo nhóm do bạn chưa tham gia vào nhóm bạn bè nào. Hãy kết nối và tham gia nhóm để cùng nhau chia sẻ phong cách thời trang nhé!")
                    .items(0)
                    .clothingItems(new ArrayList<>())
                    .build();

            return RecommendationResponseDTO.builder()
                    .userId(userId)
                    .outfit(noGroupOutfit)
                    .recommendationScore(0f)
                    .eventType("Nhóm Bạn")
                    .build();
        }

        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) {
            log.warn("Hệ thống phát hiện tủ đồ trống đối với người dùng: {}", userId);
            return createEmptyRecommendationResponse(userId, "Nhóm Bạn");
        }

        List<ClothingItemExternalDTO> rankedItems = engine.rankByCollaborative(wardrobe, groupInfo.styles);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        float realScore = calculateRealScore(finalOutfit);

        String realName = "Phong Cách " + groupInfo.groupName + " (Nhóm)";

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

    private RecommendationResponseDTO createEmptyRecommendationResponse(UUID userId, String context) {
        OutfitResponseDTO emptyOutfit = OutfitResponseDTO.builder()
                .outfitName("Tủ đồ hiện tại chưa có dữ liệu")
                .description("Bạn chưa thêm quần áo vào tủ đồ của mình. Vui lòng cập nhật thêm quần áo để AI có thể tiến hành phân tích phối đồ.")
                .items(0)
                .clothingItems(new ArrayList<>())
                .build();

        return RecommendationResponseDTO.builder()
                .userId(userId)
                .outfit(emptyOutfit)
                .recommendationScore(0f)
                .eventType(context)
                .build();
    }
}