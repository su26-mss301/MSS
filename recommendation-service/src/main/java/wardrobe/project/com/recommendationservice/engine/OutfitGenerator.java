package wardrobe.project.com.recommendationservice.engine;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutfitGenerator {

    private final RestTemplate restTemplate;

    private Map<UUID, String> fetchCategoryMap() {
        Map<UUID, String> categoryMap = new HashMap<>();
        try {
            String url = "http://localhost:8082/api/v1/wardrobe/categories";
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode dataNode = response.getBody().path("data");
                if (dataNode.isArray()) {
                    for (JsonNode node : dataNode) {
                        UUID id = UUID.fromString(node.path("categoryId").asText());
                        String name = node.path("categoryName").asText().toLowerCase();
                        categoryMap.put(id, name);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi fetch danh mục Category từ Wardrobe Service: {}", e.getMessage());
        }
        return categoryMap;
    }

    private String getBroadCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return "UNKNOWN";
        String name = categoryName.toLowerCase();

        if (name.contains("đầm") || name.contains("dress")) return "DRESS";
        if (name.contains("áo khoác") || name.contains("vest") || name.contains("blazer") || name.contains("cardigan")) return "OUTERWEAR";
        if (name.contains("áo") || name.contains("top") || name.contains("shirt")) return "TOP";
        if (name.contains("quần") || name.contains("váy") || name.contains("bottom") || name.contains("skirt")) return "BOTTOM";
        if (name.contains("giày") || name.contains("dép") || name.contains("boots")) return "SHOES";

        return "UNKNOWN";
    }

    public List<ClothingItemExternalDTO> generateBestOutfit(List<ClothingItemExternalDTO> rankedItems) {
        if (rankedItems == null || rankedItems.isEmpty()) return new ArrayList<>();

        Map<UUID, String> categoryMap = fetchCategoryMap();
        Map<String, List<ClothingItemExternalDTO>> groupedItems = new HashMap<>();

        for (ClothingItemExternalDTO item : rankedItems) {
            String catName = categoryMap.getOrDefault(item.getCategoryId(), "");
            String broadCategory = getBroadCategory(catName);
            groupedItems.computeIfAbsent(broadCategory, k -> new ArrayList<>()).add(item);
        }

        List<ClothingItemExternalDTO> finalOutfit = new ArrayList<>();

        // TRƯỜNG HỢP 1: MẶC ĐẦM
        if (groupedItems.containsKey("DRESS") && !groupedItems.get("DRESS").isEmpty()) {
            finalOutfit.add(groupedItems.get("DRESS").get(0));
        }
        // TRƯỜNG HỢP 2: MIX CƠ BẢN (1 ÁO + 1 QUẦN)
        else {
            if (groupedItems.containsKey("TOP") && !groupedItems.get("TOP").isEmpty()) {
                finalOutfit.add(groupedItems.get("TOP").get(0));
            }
            if (groupedItems.containsKey("BOTTOM") && !groupedItems.get("BOTTOM").isEmpty()) {
                finalOutfit.add(groupedItems.get("BOTTOM").get(0));
            }
        }

        if (groupedItems.containsKey("OUTERWEAR") && !groupedItems.get("OUTERWEAR").isEmpty()) {
            finalOutfit.add(groupedItems.get("OUTERWEAR").get(0));
        }

        if (groupedItems.containsKey("SHOES") && !groupedItems.get("SHOES").isEmpty()) {
            finalOutfit.add(groupedItems.get("SHOES").get(0));
        }

        if (finalOutfit.isEmpty() && groupedItems.containsKey("UNKNOWN")) {
            List<ClothingItemExternalDTO> unknowns = groupedItems.get("UNKNOWN");
            finalOutfit.add(unknowns.get(0));
            if (unknowns.size() > 1) {
                finalOutfit.add(unknowns.get(1));
            }
        }

        return finalOutfit;
    }
}