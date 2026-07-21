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
import wardrobe.project.com.recommendationservice.dto.response.RecommendationMemberOutfitDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationResponseDTO;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;
import wardrobe.project.com.recommendationservice.dto.external.CategoryDTO;
import wardrobe.project.com.recommendationservice.dto.external.UserProfileExternalDTO;
import wardrobe.project.com.recommendationservice.engine.OutfitGenerator;
import wardrobe.project.com.recommendationservice.engine.RecommendationEngine;
import wardrobe.project.com.recommendationservice.entity.Event;
import wardrobe.project.com.recommendationservice.entity.Outfit;
import wardrobe.project.com.recommendationservice.entity.OutfitItem;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.entity.RecommendMemberOutfit;
import wardrobe.project.com.recommendationservice.repository.EventRepository;
import wardrobe.project.com.recommendationservice.repository.OutfitItemRepository;
import wardrobe.project.com.recommendationservice.repository.OutfitRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendItemRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendMemberOutfitRepository;

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
    private final RecommendMemberOutfitRepository recommendMemberOutfitRepository;
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

    private String generateSharedItemsDescription(List<ClothingItemExternalDTO> items) {
        if (items == null || items.isEmpty()) {
            return "Chưa có trang phục được chia sẻ.";
        }
        String itemDetails = items.stream()
                .map(item -> item.getItemName() + " màu " + item.getDominantColor())
                .collect(Collectors.joining(", "));
        return "Trang phục đã chia sẻ trong nhóm: " + itemDetails + ".";
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

        String wardrobeUrl = apiGatewayUrl + "/api/v1/wardrobe/wardrobes/user/" + userId;

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

    @SuppressWarnings("unchecked")
    public List<ClothingItemExternalDTO> fetchGroupSharedItems(UUID groupId) {
        String url = apiGatewayUrl + "/api/v1/wardrobe/clothing-items/shared/group/" + groupId;
        HttpEntity<?> entity = new HttpEntity<>(getForwardingHeaders());

        try {
            ResponseEntity<ApiResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ApiResponse.class
            );
            ApiResponse response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> sharedItems = (List<Map<String, Object>>) response.getData();
            List<ClothingItemExternalDTO> parsedItems = new ArrayList<>();

            for (Map<String, Object> sharedMap : sharedItems) {
                ClothingItemExternalDTO itemDTO = mapSharedItemToDto(sharedMap);
                if (itemDTO.getItemId() != null) {
                    parsedItems.add(itemDTO);
                }
            }
            return parsedItems;
        } catch (Exception e) {
            log.warn("Lấy trang phục chia sẻ của nhóm {} thất bại: {}", groupId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<UUID, List<ClothingItemExternalDTO>> fetchGroupSharedItemsByMember(UUID groupId) {
        return fetchGroupSharedItems(groupId).stream()
                .filter(item -> item.getSharedByUserId() != null)
                .collect(Collectors.groupingBy(ClothingItemExternalDTO::getSharedByUserId));
    }

    private ClothingItemExternalDTO mapSharedItemToDto(Map<String, Object> sharedMap) {
        ClothingItemExternalDTO itemDTO = new ClothingItemExternalDTO();
        if (sharedMap.get("itemId") != null) {
            itemDTO.setItemId(UUID.fromString(sharedMap.get("itemId").toString()));
        }
        if (sharedMap.get("itemName") != null) {
            itemDTO.setItemName(sharedMap.get("itemName").toString());
        }
        if (sharedMap.get("dominantColor") != null) {
            itemDTO.setDominantColor(sharedMap.get("dominantColor").toString());
        }
        if (sharedMap.get("style") != null) {
            itemDTO.setStyle(sharedMap.get("style").toString());
        }
        if (sharedMap.get("imageId") != null) {
            itemDTO.setImageId(UUID.fromString(sharedMap.get("imageId").toString()));
        }
        if (sharedMap.get("confidenceScore") != null) {
            itemDTO.setConfidenceScore(Float.parseFloat(sharedMap.get("confidenceScore").toString()));
        }
        if (sharedMap.get("sharedByUserId") != null) {
            itemDTO.setSharedByUserId(UUID.fromString(sharedMap.get("sharedByUserId").toString()));
        }
        return itemDTO;
    }

    private ClothingItemExternalDTO fetchClothingItemById(UUID itemId) {
        String url = apiGatewayUrl + "/api/v1/wardrobe/clothing-items/" + itemId;
        HttpEntity<?> entity = new HttpEntity<>(getForwardingHeaders());
        try {
            ResponseEntity<ApiResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ApiResponse.class
            );
            ApiResponse response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                return null;
            }

            Map<String, Object> itemMap = (Map<String, Object>) response.getData();
            ClothingItemExternalDTO itemDTO = new ClothingItemExternalDTO();
            itemDTO.setItemId(itemId);
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
                CategoryDTO catDTO = new CategoryDTO();
                if (catMap.get("categoryId") != null) {
                    catDTO.setCategoryId(UUID.fromString(catMap.get("categoryId").toString()));
                }
                if (catMap.get("categoryName") != null) {
                    catDTO.setCategoryName(catMap.get("categoryName").toString());
                }
                itemDTO.setCategory(catDTO);
            }
            return itemDTO;
        } catch (Exception e) {
            log.warn("Không thể tải clothing item {}: {}", itemId, e.getMessage());
            return null;
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

    private static class GroupMemberInfo {
        UUID userId;
        String fullName;

        GroupMemberInfo(UUID userId, String fullName) {
            this.userId = userId;
            this.fullName = fullName;
        }
    }

    private static class GroupInfo {
        String groupName;
        List<String> styles;
        List<GroupMemberInfo> members;

        GroupInfo(String groupName, List<String> styles, List<GroupMemberInfo> members) {
            this.groupName = groupName;
            this.styles = styles;
            this.members = members;
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

                List<GroupMemberInfo> members = new ArrayList<>();
                if (membersNode.isArray()) {
                    for (JsonNode m : membersNode) {
                        String memberUserId = m.path("userId").asText(null);
                        if (memberUserId == null || memberUserId.isBlank()) {
                            continue;
                        }
                        String fullName = m.path("fullName").asText("");
                        if (fullName.isBlank()) {
                            fullName = m.path("email").asText("Thành viên");
                        }
                        members.add(new GroupMemberInfo(UUID.fromString(memberUserId), fullName));
                    }
                }

                return new GroupInfo(groupName, trendingStyles, members);
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

        List<ClothingItemExternalDTO> sharedByMember = fetchGroupSharedItems(groupId).stream()
                .filter(item -> userId.equals(item.getSharedByUserId()))
                .toList();

        if (sharedByMember.isEmpty()) {
            log.warn("Người dùng {} chưa chia sẻ trang phục nào vào nhóm {}", userId, groupId);
            return createNoGroupSharedItemsResponse(userId);
        }

        List<ClothingItemExternalDTO> rankedItems = engine.rankByCollaborative(sharedByMember, groupInfo.styles);
        List<ClothingItemExternalDTO> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        if (!outfitGenerator.isValidOutfit(finalOutfit)) {
            log.warn("Set đồ nhóm không đủ thành phần từ trang phục đã chia sẻ cho user: {}", userId);
            return createNotEnoughSharedItemsResponse(userId);
        }

        float realScore = calculateRealScore(finalOutfit);

        String realName = groupInfo.groupName + " (Nhóm)";
        String realDesc = generateDynamicDescription(finalOutfit);

        Event event = getOrCreateEvent("Party");
        Map<UUID, List<ClothingItemExternalDTO>> sharedItemsByMember = fetchGroupSharedItemsByMember(groupId);
        RecommendItem entity = saveGroupRecommendation(
                userId,
                groupId,
                groupInfo,
                sharedItemsByMember,
                event,
                realName,
                realDesc
        );
        return mapToResponse(entity, sharedByMember);
    }

    private RecommendItem saveGroupRecommendation(
            UUID creatorUserId,
            UUID groupId,
            GroupInfo groupInfo,
            Map<UUID, List<ClothingItemExternalDTO>> sharedItemsByMember,
            Event event,
            String name,
            String description
    ) {
        Map<UUID, GeneratedMemberOutfit> generatedOutfits = new LinkedHashMap<>();

        for (GroupMemberInfo member : groupInfo.members) {
            List<ClothingItemExternalDTO> memberShared = sharedItemsByMember.getOrDefault(member.userId, List.of());
            if (memberShared.isEmpty()) {
                log.info("Bỏ qua thành viên {} do chưa chia sẻ trang phục vào nhóm", member.userId);
                continue;
            }

            List<ClothingItemExternalDTO> memberRanked = engine.rankByCollaborative(memberShared, groupInfo.styles);
            List<ClothingItemExternalDTO> displayItems = memberRanked.isEmpty() ? memberShared : memberRanked;

            float memberScore = calculateRealScore(displayItems);
            String memberDesc = generateSharedItemsDescription(displayItems);
            Outfit memberOutfit = createOutfitWithItems(
                    name + " · " + member.fullName,
                    memberDesc,
                    displayItems
            );
            generatedOutfits.put(member.userId, new GeneratedMemberOutfit(member.fullName, memberOutfit, memberScore));
        }

        GeneratedMemberOutfit creatorOutfit = generatedOutfits.get(creatorUserId);
        if (creatorOutfit == null) {
            throw new RuntimeException("Không thể tạo gợi ý nhóm từ trang phục đã chia sẻ");
        }

        List<ClothingItemExternalDTO> creatorShared = sharedItemsByMember.getOrDefault(creatorUserId, List.of());
        List<ClothingItemExternalDTO> creatorRanked = engine.rankByCollaborative(creatorShared, groupInfo.styles);
        List<ClothingItemExternalDTO> creatorBestOutfit = outfitGenerator.generateBestOutfit(creatorRanked);
        if (creatorBestOutfit.isEmpty()) {
            creatorBestOutfit = creatorRanked.isEmpty() ? creatorShared : creatorRanked;
        }

        Outfit mainOutfit = createOutfitWithItems(name, description, creatorBestOutfit);

        RecommendItem rec = new RecommendItem();
        rec.setUserId(creatorUserId);
        rec.setOutfit(mainOutfit);
        rec.setEvent(event);
        rec.setRecommendationScore(calculateRealScore(creatorBestOutfit));
        rec.setGroupId(groupId);
        rec.setGroupName(groupInfo.groupName);
        rec.setGroupStyles(String.join(",", groupInfo.styles));
        rec = recommendItemRepository.save(rec);

        for (Map.Entry<UUID, GeneratedMemberOutfit> entry : generatedOutfits.entrySet()) {
            GeneratedMemberOutfit generated = entry.getValue();
            saveMemberOutfit(rec, entry.getKey(), generated.fullName(), generated.outfit(), generated.score());
        }

        return rec;
    }

    private record GeneratedMemberOutfit(String fullName, Outfit outfit, float score) {}

    private void saveMemberOutfit(
            RecommendItem recommendItem,
            UUID memberUserId,
            String memberName,
            Outfit outfit,
            float memberScore
    ) {
        RecommendMemberOutfit memberRecord = new RecommendMemberOutfit();
        memberRecord.setRecommendItem(recommendItem);
        memberRecord.setMemberUserId(memberUserId);
        memberRecord.setMemberName(memberName);
        memberRecord.setOutfit(outfit);
        memberRecord.setMemberScore(memberScore);
        recommendMemberOutfitRepository.save(memberRecord);
    }

    private String resolveMemberName(GroupInfo groupInfo, UUID userId) {
        return groupInfo.members.stream()
                .filter(member -> member.userId.equals(userId))
                .map(member -> member.fullName)
                .findFirst()
                .orElse("Thành viên");
    }

    private Outfit createOutfitWithItems(String name, String description, List<ClothingItemExternalDTO> items) {
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

        return outfit;
    }

    private RecommendItem saveRecommendation(UUID userId, List<ClothingItemExternalDTO> items, Event event, float score, String name, String description) {
        Outfit outfit = createOutfitWithItems(name, description, items);

        RecommendItem rec = new RecommendItem();
        rec.setUserId(userId);
        rec.setOutfit(outfit);
        rec.setEvent(event);
        rec.setRecommendationScore(score);
        return recommendItemRepository.save(rec);
    }

    public OutfitResponseDTO buildOutfitResponse(Outfit outfit, UUID memberUserId, RecommendItem recommendItem) {
        List<ClothingItemExternalDTO> availableItems = resolveAvailableItems(recommendItem, memberUserId);
        List<OutfitItem> outfitItems = outfitItemRepository.findByOutfit(outfit);
        List<ClothingItemExternalDTO> realClothingDetails = resolveClothingDetails(outfitItems, availableItems);
        return toOutfitResponseDto(outfit, recommendItem, realClothingDetails);
    }

    public OutfitResponseDTO buildOutfitResponseForAdmin(Outfit outfit, RecommendItem recommendItem) {
        List<OutfitItem> outfitItems = outfitItemRepository.findByOutfit(outfit);
        List<ClothingItemExternalDTO> realClothingDetails = resolveClothingDetailsByItemIds(outfitItems);
        return toOutfitResponseDto(outfit, recommendItem, realClothingDetails);
    }

    public List<RecommendationMemberOutfitDTO> buildAdminGroupMemberDetails(
            RecommendItem item,
            List<RecommendMemberOutfit> savedMemberOutfits
    ) {
        Map<UUID, RecommendMemberOutfit> savedByUserId = savedMemberOutfits.stream()
                .collect(Collectors.toMap(
                        RecommendMemberOutfit::getMemberUserId,
                        member -> member,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<UUID, List<ClothingItemExternalDTO>> sharedByMember = item.getGroupId() != null
                ? fetchGroupSharedItemsByMember(item.getGroupId())
                : Map.of();

        LinkedHashSet<UUID> orderedUserIds = new LinkedHashSet<>();
        for (RecommendMemberOutfit saved : savedMemberOutfits) {
            orderedUserIds.add(saved.getMemberUserId());
        }
        for (Map.Entry<UUID, List<ClothingItemExternalDTO>> entry : sharedByMember.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                orderedUserIds.add(entry.getKey());
            }
        }

        List<String> groupStyles = parseGroupStyles(item.getGroupStyles());
        List<RecommendationMemberOutfitDTO> members = new ArrayList<>();

        for (UUID memberUserId : orderedUserIds) {
            RecommendMemberOutfit saved = savedByUserId.get(memberUserId);
            List<ClothingItemExternalDTO> sharedItems = sharedByMember.getOrDefault(memberUserId, List.of());

            OutfitResponseDTO outfitDto;
            float score;
            String fullName;

            if (!sharedItems.isEmpty()) {
                List<ClothingItemExternalDTO> rankedShared = groupStyles.isEmpty()
                        ? sharedItems
                        : engine.rankByCollaborative(sharedItems, groupStyles);
                outfitDto = buildOutfitResponseFromClothingItems(
                        item,
                        rankedShared,
                        item.getGroupName() != null ? item.getGroupName() + " (Nhóm)" : "Trang phục nhóm"
                );
                score = saved != null && saved.getMemberScore() != null
                        ? saved.getMemberScore()
                        : calculateRealScore(rankedShared);
                fullName = saved != null && saved.getMemberName() != null && !saved.getMemberName().isBlank()
                        ? saved.getMemberName()
                        : "Thành viên";
            } else if (saved != null) {
                outfitDto = buildOutfitResponseForAdmin(saved.getOutfit(), item);
                score = saved.getMemberScore() != null ? saved.getMemberScore() : 0f;
                fullName = saved.getMemberName() != null ? saved.getMemberName() : "Thành viên";
            } else {
                continue;
            }

            if (outfitDto.getClothingItems() == null || outfitDto.getClothingItems().isEmpty()) {
                continue;
            }

            members.add(RecommendationMemberOutfitDTO.builder()
                    .userId(memberUserId)
                    .fullName(fullName)
                    .recommendationScore(score)
                    .outfit(outfitDto)
                    .creator(item.getUserId().equals(memberUserId))
                    .build());
        }

        return members;
    }

    public OutfitResponseDTO buildOutfitResponseFromClothingItems(
            RecommendItem recommendItem,
            List<ClothingItemExternalDTO> clothingItems,
            String outfitName
    ) {
        List<ClothingItemExternalDTO> resolvedItems = clothingItems.stream()
                .map(item -> {
                    if (item.getImageId() != null && item.getItemName() != null) {
                        return item;
                    }
                    ClothingItemExternalDTO fetched = fetchClothingItemById(item.getItemId());
                    return fetched != null ? fetched : item;
                })
                .filter(Objects::nonNull)
                .toList();

        List<String> realTags = new ArrayList<>();
        if (recommendItem != null && recommendItem.getEvent() != null && recommendItem.getEvent().getEventType() != null) {
            realTags.add(recommendItem.getEvent().getEventType());
        }

        return OutfitResponseDTO.builder()
                .outfitName(outfitName)
                .description(generateSharedItemsDescription(resolvedItems))
                .img(null)
                .items(resolvedItems.size())
                .tags(realTags)
                .clothingItems(resolvedItems)
                .build();
    }

    private OutfitResponseDTO toOutfitResponseDto(
            Outfit outfit,
            RecommendItem recommendItem,
            List<ClothingItemExternalDTO> realClothingDetails
    ) {
        List<String> realTags = new ArrayList<>();
        if (recommendItem != null && recommendItem.getEvent() != null && recommendItem.getEvent().getEventType() != null) {
            realTags.add(recommendItem.getEvent().getEventType());
        }

        return OutfitResponseDTO.builder()
                .outfitId(outfit.getId())
                .outfitName(outfit.getOutfitName())
                .description(outfit.getDescription())
                .img(null)
                .items(realClothingDetails.size())
                .tags(realTags)
                .clothingItems(realClothingDetails)
                .build();
    }

    private List<ClothingItemExternalDTO> resolveClothingDetailsByItemIds(List<OutfitItem> outfitItems) {
        List<ClothingItemExternalDTO> realClothingDetails = new ArrayList<>();
        if (outfitItems == null) {
            return realClothingDetails;
        }

        for (OutfitItem outfitItem : outfitItems) {
            ClothingItemExternalDTO matched = fetchClothingItemById(outfitItem.getItemId());
            if (matched != null) {
                realClothingDetails.add(matched);
            }
        }
        return realClothingDetails;
    }

    public List<String> parseGroupStyles(String groupStyles) {
        if (groupStyles == null || groupStyles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(groupStyles.split(","))
                .map(String::trim)
                .filter(style -> !style.isEmpty())
                .toList();
    }

    private List<ClothingItemExternalDTO> resolveAvailableItems(RecommendItem recommendItem, UUID memberUserId) {
        if (recommendItem != null && recommendItem.getGroupId() != null) {
            List<ClothingItemExternalDTO> sharedItems = fetchGroupSharedItems(recommendItem.getGroupId()).stream()
                    .filter(item -> memberUserId.equals(item.getSharedByUserId()))
                    .toList();
            if (!sharedItems.isEmpty()) {
                return sharedItems;
            }
        }
        return fetchUserWardrobe(memberUserId);
    }

    private List<ClothingItemExternalDTO> resolveClothingDetails(
            List<OutfitItem> outfitItems,
            List<ClothingItemExternalDTO> availableItems
    ) {
        List<ClothingItemExternalDTO> realClothingDetails = new ArrayList<>();
        if (outfitItems == null) {
            return realClothingDetails;
        }

        for (OutfitItem outfitItem : outfitItems) {
            ClothingItemExternalDTO matched = null;
            if (availableItems != null) {
                matched = availableItems.stream()
                        .filter(item -> item.getItemId().equals(outfitItem.getItemId()))
                        .findFirst()
                        .orElse(null);
            }
            if (matched == null) {
                matched = fetchClothingItemById(outfitItem.getItemId());
            }
            if (matched != null) {
                realClothingDetails.add(matched);
            }
        }
        return realClothingDetails;
    }

    private RecommendationResponseDTO mapToResponse(RecommendItem item, List<ClothingItemExternalDTO> availableItems) {
        List<OutfitItem> outfitItems = outfitItemRepository.findByOutfit(item.getOutfit());
        List<ClothingItemExternalDTO> realClothingDetails = resolveClothingDetails(outfitItems, availableItems);

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

    private RecommendationResponseDTO createNoGroupSharedItemsResponse(UUID userId) {
        OutfitResponseDTO emptyOutfit = OutfitResponseDTO.builder()
                .outfitName("Chưa có trang phục chia sẻ")
                .description("Hãy chia sẻ trang phục vào nhóm bạn trước khi tạo gợi ý nhóm. Mỗi thành viên cần chia sẻ đồ của mình để AI phối theo phong cách chung của nhóm.")
                .items(0)
                .clothingItems(new ArrayList<>())
                .build();

        return RecommendationResponseDTO.builder()
                .userId(userId)
                .outfit(emptyOutfit)
                .recommendationScore(0f)
                .eventType("Nhóm Bạn")
                .build();
    }

    private RecommendationResponseDTO createNotEnoughSharedItemsResponse(UUID userId) {
        OutfitResponseDTO emptyOutfit = OutfitResponseDTO.builder()
                .outfitName("Trang phục chia sẻ chưa đủ")
                .description("Trang phục bạn đã chia sẻ vào nhóm chưa đủ thành phần cơ bản để phối đồ (Cần ít nhất Áo + Quần/Váy, hoặc Đầm liền). Hãy chia sẻ thêm trang phục phù hợp nhé!")
                .items(0)
                .clothingItems(new ArrayList<>())
                .build();

        return RecommendationResponseDTO.builder()
                .userId(userId)
                .outfit(emptyOutfit)
                .recommendationScore(0f)
                .eventType("Nhóm Bạn")
                .build();
    }
}