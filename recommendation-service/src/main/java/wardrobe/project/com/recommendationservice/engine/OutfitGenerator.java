package wardrobe.project.com.recommendationservice.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutfitGenerator {

    // Quy tắc phân nhóm: Dùng substring để phân loại đồ vào các "Layer" (Tầng)
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

        Map<String, List<ClothingItemExternalDTO>> groupedItems = new HashMap<>();

        for (ClothingItemExternalDTO item : rankedItems) {
            String catName = (item.getCategory() != null) ? item.getCategory().getCategoryName() : "";
            String broadCategory = getBroadCategory(catName);
            groupedItems.computeIfAbsent(broadCategory, k -> new ArrayList<>()).add(item);
        }

        List<ClothingItemExternalDTO> finalOutfit = new ArrayList<>();

        if (groupedItems.containsKey("DRESS") && !groupedItems.get("DRESS").isEmpty()) {
            finalOutfit.add(groupedItems.get("DRESS").get(0));
        } else {
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

        if (finalOutfit.size() < 3) {
            for (ClothingItemExternalDTO item : rankedItems) {
                if (finalOutfit.size() >= 3) break;
                if (!finalOutfit.contains(item)) {
                    finalOutfit.add(item);
                }
            }
        }

        return finalOutfit;
    }
}