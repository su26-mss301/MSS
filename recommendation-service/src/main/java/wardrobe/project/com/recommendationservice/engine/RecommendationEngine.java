package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;
import wardrobe.project.com.recommendationservice.dto.external.UserProfileExternalDTO;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationEngine {

    private static final Map<String, List<String>> COLOR_DICTIONARY = Map.ofEntries(
            Map.entry("#000000", List.of("đen", "black")),
            Map.entry("#FFFFFF", List.of("trắng", "white")),
            Map.entry("#1E3A5F", List.of("xanh đậm", "xanh dương", "blue", "navy")),
            Map.entry("#EA580C", List.of("chàm", "cam", "orange")),
            Map.entry("#F97316", List.of("tím", "cam", "purple", "orange")),
            Map.entry("#EC4899", List.of("hồng", "pink")),
            Map.entry("#EF4444", List.of("đỏ", "red")),
            Map.entry("#F59E0B", List.of("vàng", "yellow")),
            Map.entry("#10B981", List.of("xanh lá", "green")),
            Map.entry("#14B8A6", List.of("mòng két", "teal")),
            Map.entry("#94A3B8", List.of("xám", "gray", "grey", "gainsboro")),
            Map.entry("#92400E", List.of("nâu", "brown")),
            Map.entry("#D4B896", List.of("be", "beige"))
    );

    private static final Map<String, List<String>> STYLE_DICTIONARY = Map.of(
            "minimal", List.of("tối giản", "minimal"),
            "casual", List.of("thường ngày", "casual", "hằng ngày", "đi chơi"),
            "business", List.of("công sở", "business", "lịch sự thoải mái", "họp"),
            "formal", List.of("trang trọng", "formal", "lịch sự", "thanh lịch"),
            "streetwear", List.of("đường phố", "streetwear", "bụi bặm"),
            "sporty", List.of("thể thao", "sport", "năng động")
    );

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    public List<ClothingItemExternalDTO> rankByContentBased(List<ClothingItemExternalDTO> wardrobe, UserProfileExternalDTO profile) {
        Map<ClothingItemExternalDTO, Float> scoredItems = new HashMap<>();

        for (ClothingItemExternalDTO item : wardrobe) {
            float score = 1.0f;

            if (profile.getPreferredStyle() != null && item.getStyle() != null) {
                String profileStyleKey = profile.getPreferredStyle().toLowerCase();
                String itemStyle = item.getStyle().toLowerCase();

                List<String> validKeywords = STYLE_DICTIONARY.getOrDefault(profileStyleKey, List.of(profileStyleKey));
                boolean isStyleMatch = validKeywords.stream().anyMatch(itemStyle::contains);

                if (isStyleMatch) score += 5.0f;
            }

            if (profile.getFavoriteColors() != null && item.getDominantColor() != null) {
                String itemColor = item.getDominantColor().toLowerCase();
                boolean isColorMatch = false;

                for (String hexColor : profile.getFavoriteColors()) {
                    List<String> validColorNames = COLOR_DICTIONARY.getOrDefault(hexColor.toUpperCase(), List.of());
                    if (validColorNames.stream().anyMatch(itemColor::contains)) {
                        isColorMatch = true;
                        break;
                    }
                }

                if (isColorMatch) score += 3.0f;
            }

            score += (float) (Math.random() * 0.1);
            scoredItems.put(item, score);
        }

        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<ClothingItemExternalDTO> filterByEvent(List<ClothingItemExternalDTO> wardrobe, String eventType) {
        Map<ClothingItemExternalDTO, Float> scoredItems = new HashMap<>();

        for (ClothingItemExternalDTO item : wardrobe) {
            if (item.getStyle() == null) continue;
            String itemStyle = item.getStyle().toLowerCase();
            float score = 0f;

            switch (eventType.toLowerCase()) {
                case "wedding", "meeting", "interview" -> {
                    if (containsAny(itemStyle, "formal", "trang trọng", "business", "công sở")) score += 5.0f;
                    else if (containsAny(itemStyle, "thanh lịch", "lịch sự")) score += 2.0f;

                    if (containsAny(itemStyle, "thường ngày", "casual", "đường phố", "thể thao")) score -= 5.0f;
                }
                case "party" -> {
                    if (containsAny(itemStyle, "party", "tiệc tùng", "nổi bật")) score += 5.0f;
                    else if (containsAny(itemStyle, "đường phố", "streetwear", "cá tính")) score += 3.0f;
                    else if (containsAny(itemStyle, "casual", "thường ngày")) score += 1.0f;

                    if (containsAny(itemStyle, "công sở", "business", "formal")) score -= 3.0f;
                }
                default -> {
                    if (containsAny(itemStyle, "casual", "thường ngày", "hằng ngày", "thoải mái", "tối giản")) score += 5.0f;
                    else if (containsAny(itemStyle, "thể thao", "năng động", "sport")) score += 3.0f;
                    else if (containsAny(itemStyle, "lịch sự thoải mái", "smart casual")) score += 1.0f;

                    if (containsAny(itemStyle, "formal", "trang trọng", "công sở", "business")) score -= 5.0f;
                }
            }

            if (score > 0) {
                score += (float) (Math.random() * 0.1);
                scoredItems.put(item, score);
            }
        }

        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<ClothingItemExternalDTO> rankByCollaborative(List<ClothingItemExternalDTO> wardrobe, List<String> trendingStyles) {
        Map<ClothingItemExternalDTO, Float> scoredItems = new HashMap<>();

        for (ClothingItemExternalDTO item : wardrobe) {
            float score = 1.0f;

            if (item.getStyle() != null) {
                String itemStyle = item.getStyle().toLowerCase();
                boolean isTrending = trendingStyles.stream()
                        .map(String::toLowerCase)
                        .anyMatch(itemStyle::contains);

                if (isTrending) score += 4.0f;
            }

            score += (float) (Math.random() * 0.1);
            scoredItems.put(item, score);
        }

        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}