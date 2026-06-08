package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.ClothingItemDto;
import wardrobe.project.com.recommendationservice.dto.UserProfileDto;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationEngine {

    // CONTENT-BASED (Chấm điểm theo sở thích)
    public List<ClothingItemDto> rankByContentBased(List<ClothingItemDto> wardrobe, UserProfileDto profile) {
        // Sử dụng Map để lưu trữ điểm số của từng món đồ
        Map<ClothingItemDto, Float> scoredItems = new HashMap<>();

        for (ClothingItemDto item : wardrobe) {
            float score = 0.0f;

            // Tiêu chí 1: Phong cách (Style) - Quan trọng nhất (Trọng số cao)
            if (item.getStyle() != null && item.getStyle().equalsIgnoreCase(profile.getPreferredStyle())) {
                score += 5.0f;
            }

            // Tiêu chí 2: Màu sắc (Color) - Quan trọng nhì
            if (item.getColor() != null && profile.getFavoriteColors().contains(item.getColor())) {
                score += 3.0f;
            }

            // Chỉ lấy những đồ có điểm (phù hợp ít nhất 1 tiêu chí)
            if (score > 0) {
                scoredItems.put(item, score);
            }
        }

        // Sắp xếp giảm dần theo điểm số và trả về danh sách
        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    // EVENT-BASED (Lọc theo tính chất sự kiện)
    public List<ClothingItemDto> filterByEvent(List<ClothingItemDto> wardrobe, String eventType) {
        List<String> targetStyles = new ArrayList<>();

        switch (eventType.toLowerCase()) {
            case "wedding":
            case "meeting":
            case "interview":
                targetStyles.add("Formal");
                targetStyles.add("Business Casual");
                break;
            case "party":
                targetStyles.add("Streetwear");
                targetStyles.add("Casual");
                break;
            case "sports":
                targetStyles.add("Sport");
                break;
            default:
                targetStyles.add("Casual"); // Mặc định
        }

        return wardrobe.stream()
                .filter(item -> targetStyles.contains(item.getStyle()))
                .collect(Collectors.toList());
    }


    // COLLABORATIVE FILTERING (Gợi ý theo Trend nhóm bạn)
    // Giả sử chúng ta đã phân tích lịch sử của nhóm bạn và tìm ra được "Trending Styles" và "Trending Colors"
    public List<ClothingItemDto> rankByCollaborative(List<ClothingItemDto> wardrobe, List<String> trendingStylesInGroup) {
        Map<ClothingItemDto, Float> scoredItems = new HashMap<>();

        for (ClothingItemDto item : wardrobe) {
            float score = 0.0f;

            // Nếu món đồ của user khớp với xu hướng (trend) mà bạn bè đang mặc nhiều
            if (trendingStylesInGroup.contains(item.getStyle())) {
                score += 4.0f; // Điểm cộng vì hợp trend
            }

            scoredItems.put(item, score);
        }

        // Trả về những món đồ hot nhất xếp trên cùng
        return scoredItems.entrySet().stream()
                .sorted((e1, e2) -> Float.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}