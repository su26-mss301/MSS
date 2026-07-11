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

    public List<ClothingItemExternalDTO> generateBestOutfit(List<ClothingItemExternalDTO> rankedItems) {
        if (rankedItems == null || rankedItems.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<ClothingItemExternalDTO>> groupedItems = rankedItems.stream()
                .filter(item -> item.getCategory() != null && item.getCategory().getCategoryName() != null)
                .collect(Collectors.groupingBy(item -> item.getCategory().getCategoryName()));

        List<ClothingItemExternalDTO> finalOutfit = new ArrayList<>();
        Set<String> addedCategories = new HashSet<>();

        for (List<ClothingItemExternalDTO> itemsInCategory : groupedItems.values()) {
            if (finalOutfit.size() >= 3) break;

            ClothingItemExternalDTO item = itemsInCategory.get(0);
            finalOutfit.add(item);
            addedCategories.add(item.getCategory().getCategoryName());
        }

        if (finalOutfit.size() < 3) {
            Set<String> addedIds = finalOutfit.stream()
                    .map(item -> item.getItemId().toString())
                    .collect(Collectors.toSet());

            for (ClothingItemExternalDTO item : rankedItems) {
                if (finalOutfit.size() >= 3) break;

                if (!addedIds.contains(item.getItemId().toString())) {
                    finalOutfit.add(item);
                    addedIds.add(item.getItemId().toString());
                }
            }
        }

        return finalOutfit;
    }
}