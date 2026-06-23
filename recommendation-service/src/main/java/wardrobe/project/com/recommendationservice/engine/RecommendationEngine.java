package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;
import wardrobe.project.com.recommendationservice.dto.external.UserProfileExternalDTO;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationEngine {

    public List<ClothingItemExternalDTO> rankByContentBased(List<ClothingItemExternalDTO> wardrobe, UserProfileExternalDTO profile) {
        Map<ClothingItemExternalDTO, Float> scoredItems = new HashMap<>();
        for (ClothingItemExternalDTO item : wardrobe) {
            float score = 1.0f;

            if (profile.getPreferredStyle() != null && item.getStyle() != null &&
                    item.getStyle().equalsIgnoreCase(profile.getPreferredStyle())) {
                score += 5.0f;
            }

            if (profile.getFavoriteColors() != null && item.getDominantColor() != null &&
                    profile.getFavoriteColors().contains(item.getDominantColor())) {
                score += 3.0f;
            }

            scoredItems.put(item, score);
        }

        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<ClothingItemExternalDTO> filterByEvent(List<ClothingItemExternalDTO> wardrobe, String eventType) {
        List<String> targetStyles = new ArrayList<>();
        switch (eventType.toLowerCase()) {
            case "wedding", "meeting", "interview" -> {
                targetStyles.add("Formal");
                targetStyles.add("Business Casual");
            }
            case "party" -> {
                targetStyles.add("Streetwear");
                targetStyles.add("Casual");
            }
            default -> targetStyles.add("Casual");
        }
        return wardrobe.stream()
                .filter(item -> targetStyles.contains(item.getStyle()))
                .collect(Collectors.toList());
    }

    public List<ClothingItemExternalDTO> rankByCollaborative(List<ClothingItemExternalDTO> wardrobe, List<String> trendingStyles) {
        Map<ClothingItemExternalDTO, Float> scoredItems = new HashMap<>();
        for (ClothingItemExternalDTO item : wardrobe) {
            float score = 1.0f;

            if (item.getStyle() != null && trendingStyles.contains(item.getStyle())) {
                score += 4.0f;
            }
            scoredItems.put(item, score);
        }

        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}