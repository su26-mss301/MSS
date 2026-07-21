package wardrobe.project.com.recommendationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService executorService = Executors.newFixedThreadPool(15);

    @Value("${app.services.api-gateway-url}")
    private String apiGatewayUrl;

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

    private HttpHeaders getForwardingHeaders() {
        HttpHeaders headers = new HttpHeaders();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null) {
                    headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                }

                String cookieHeader = request.getHeader(HttpHeaders.COOKIE);
                if (cookieHeader != null) {
                    headers.set(HttpHeaders.COOKIE, cookieHeader);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi lấy request attributes: ", e);
        }

        AuthContext authContext = AuthContextHolder.getNullable();
        if (authContext != null) {
            if (authContext.getUserId() != null) headers.set("X-Auth-User-Id", authContext.getUserId());
            if (authContext.getActorType() != null) headers.set("X-Auth-Actor-Type", authContext.getActorType().name());
            if (authContext.getRole() != null) headers.set("X-Auth-Roles", authContext.getRole().name());
        }

        return headers;
    }

    private UserProfileExternalDTO fetchUserProfile(UUID userId) {
        UserProfileExternalDTO profile = new UserProfileExternalDTO();
        profile.setId(userId);
        try {
            String url = apiGatewayUrl + "/api/v1/users/style-preferences/me";
            HttpEntity<?> entity = new HttpEntity<>(getForwardingHeaders());

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

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

    @SuppressWarnings("unchecked")
    public List<ClothingItemExternalDTO> fetchUserWardrobe(UUID userId) {

        String wardrobeUrl = apiGatewayUrl + "/api/v1/wardrobe/wardrobes";

        HttpEntity<?> entity = new HttpEntity<>(getForwardingHeaders());

        try {
            ResponseEntity<ApiResponse> responseEntity = restTemplate.exchange(
                    wardrobeUrl, HttpMethod.GET, entity, ApiResponse.class
            );
            ApiResponse response = responseEntity.getBody();

            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> wardrobes = (List<Map<String, Object>>) response.getData();
            if (wardrobes.isEmpty()) {
                return Collections.emptyList();
            }

            List<CompletableFuture<List<ClothingItemExternalDTO>>> asyncTasks = new ArrayList<>();

            for (Map<String, Object> wardrobe : wardrobes) {
                Object wIdObj = wardrobe.get("wardrobeId");
                if (wIdObj == null) wIdObj = wardrobe.get("id");
                if (wIdObj == null) continue;

                String wardrobeId = wIdObj.toString();
                String zoneUrl = apiGatewayUrl + "/api/v1/wardrobe/wardrobe-zones/wardrobe/" + wardrobeId;

                ResponseEntity<ApiResponse> zoneResponseEntity = restTemplate.exchange(
                        zoneUrl, HttpMethod.GET, entity, ApiResponse.class
                );
                ApiResponse zoneResponse = zoneResponseEntity.getBody();

                if (zoneResponse == null || zoneResponse.getData() == null) continue;

                List<Map<String, Object>> zones = (List<Map<String, Object>>) zoneResponse.getData();

                for (Map<String, Object> zone : zones) {
                    Object zIdObj = zone.get("zoneId");
                    if (zIdObj == null) zIdObj = zone.get("id");
                    if (zIdObj == null) continue;

                    String zoneId = zIdObj.toString();

                    CompletableFuture<List<ClothingItemExternalDTO>> futureTask = CompletableFuture.supplyAsync(() -> {
                        String itemUrl = apiGatewayUrl + "/api/v1/wardrobe/clothing-items/zone/" + zoneId;
                        try {
                            ResponseEntity<ApiResponse> itemResponseEntity = restTemplate.exchange(
                                    itemUrl, HttpMethod.GET, entity, ApiResponse.class
                            );
                            ApiResponse itemResponse = itemResponseEntity.getBody();

                            if (itemResponse == null || itemResponse.getData() == null) {
                                return Collections.emptyList();
                            }

                            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) itemResponse.getData();
                            List<ClothingItemExternalDTO> parsedItems = new ArrayList<>();

                            for (Map<String, Object> itemMap : itemsList) {
                                ClothingItemExternalDTO itemDTO = new ClothingItemExternalDTO();
                                if (itemMap.get("itemId") != null) {
                                    itemDTO.setItemId(UUID.fromString(itemMap.get("itemId").toString()));
                                }
                                if (itemMap.get("zoneId") != null) {
                                    itemDTO.setZoneId(UUID.fromString(itemMap.get("zoneId").toString()));
                                }
                                if (itemMap.get("itemName") != null) {
                                    itemDTO.setItemName(itemMap.get("itemName").toString());
                                }
                                if (itemMap.get("dominantColor") != null) {
                                    itemDTO.setDominantColor(itemMap.get("dominantColor").toString());
                                }
                                if (itemMap.get("style") != null) {
                                    itemDTO.setStyle(itemMap.get("style").toString());
                                }
                                if (itemMap.get("imageId") != null) {
                                    itemDTO.setImageId(UUID.fromString(itemMap.get("imageId").toString()));
                                }
                                if (itemMap.get("confidenceScore") != null) {
                                    itemDTO.setConfidenceScore(Float.parseFloat(itemMap.get("confidenceScore").toString()));
                                }

                                if (itemMap.get("category") != null) {
                                    Map<String, Object> catMap = (Map<String, Object>) itemMap.get("category");
                                    wardrobe.project.com.recommendationservice.dto.external.CategoryDTO catDTO = new wardrobe.project.com.recommendationservice.dto.external.CategoryDTO();
                                    if (catMap.get("categoryId") != null) {
                                        catDTO.setCategoryId(UUID.fromString(catMap.get("categoryId").toString()));
                                    }
                                    if (catMap.get("categoryName") != null) {
                                        catDTO.setCategoryName(catMap.get("categoryName").toString());
                                    }
                                    itemDTO.setCategory(catDTO);
                                }
                                parsedItems.add(itemDTO);
                            }
                            return parsedItems;
                        } catch (Exception e) {
                            log.error("Lỗi khi fetch items cho zone: " + zoneId, e);
                            return Collections.emptyList();
                        }
                    }, executorService);

                    asyncTasks.add(futureTask);
                }
            }

            return asyncTasks.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Lỗi hệ thống khi tải tủ đồ của người dùng: {}", userId, e);
            return Collections.emptyList();
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
            String url = apiGatewayUrl + "/api/v1/users/friend-groups/" + groupId + "/detail";
            HttpEntity<?> entity = new HttpEntity<>(getForwardingHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

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
            String url = apiGatewayUrl + "/api/v1/users/friend-groups/" + groupId + "/detail";
            HttpEntity<?> entity = new HttpEntity<>(getForwardingHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

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
    public RecommendationResponseDTO generateContentBased(UUID userId, String chosenStyle) {
        List<ClothingItemExternalDTO> wardrobe = fetchUserWardrobe(userId);
        if (wardrobe.isEmpty()) {
            log.warn("Hệ thống phát hiện tủ đồ trống đối với người dùng: {}", userId);
            return createEmptyRecommendationResponse(userId, "Cá Nhân");
        }

        List<String> styles = new ArrayList<>();
        if (chosenStyle != null && !chosenStyle.trim().isEmpty()) {
            styles.add(chosenStyle.toLowerCase().trim());
        } else {
            styles.add("thường ngày");
        }

        List<ClothingItemExternalDTO> filteredWardrobe = new ArrayList<>();
        if (chosenStyle != null && !chosenStyle.trim().isEmpty()) {
            String targetStyle = chosenStyle.toLowerCase().trim();
            for (ClothingItemExternalDTO item : wardrobe) {
                String itemName = item.getItemName() != null ? item.getItemName().toLowerCase() : "";
                String catName = item.getCategory() != null ? item.getCategory().getCategoryName().toLowerCase() : "";

                if (itemName.contains(targetStyle) || catName.contains(targetStyle)) {
                    filteredWardrobe.add(item);
                }
            }
        }

        List<ClothingItemExternalDTO> targetWardrobe = filteredWardrobe.isEmpty() ? wardrobe : filteredWardrobe;

        List<ClothingItemExternalDTO> rankedItems = engine.rankByPersonal(targetWardrobe, styles);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        if (!outfitGenerator.isValidOutfit(finalOutfit)) {
            log.warn("Set đồ cá nhân không đủ thành phần cơ bản sau khi lọc phong cách cho user: {}", userId);
            return createNotEnoughItemsResponse(userId, "Cá Nhân");
        }

        float realScore = calculateRealScore(finalOutfit);

        String finalOutfitName = "Phong Cách " + (chosenStyle != null ? chosenStyle : "Cá Nhân") + " (Cá Nhân)";
        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent("Personal");
        RecommendItem entity = saveRecommendation(userId, finalOutfit, event, realScore, finalOutfitName, realDesc);
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

        if (!outfitGenerator.isValidOutfit(finalOutfit)) {
            log.warn("Set đồ event ({}) không đủ thành phần cơ bản cho user: {}", eventType, userId);
            return createNotEnoughItemsResponse(userId, eventType);
        }

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

        if (!outfitGenerator.isValidOutfit(finalOutfit)) {
            log.warn("Set đồ nhóm không đủ thành phần cơ bản cho user: {}", userId);
            return createNotEnoughItemsResponse(userId, "Nhóm Bạn");
        }

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

    private RecommendationResponseDTO createNotEnoughItemsResponse(UUID userId, String eventType) {
        OutfitResponseDTO emptyOutfit = OutfitResponseDTO.builder()
                .outfitName("Thiếu trang phục phù hợp")
                .description("Tủ đồ của bạn không có đủ trang phục cơ bản (Cần ít nhất Áo + Quần/Váy, hoặc Đầm liền) phù hợp cho dịp này. Hãy chụp và thêm đồ vào tủ nhé!")
                .items(0)
                .clothingItems(new ArrayList<>())
                .build();

        return RecommendationResponseDTO.builder()
                .userId(userId)
                .outfit(emptyOutfit)
                .recommendationScore(0f)
                .eventType(eventType)
                .build();
    }
}