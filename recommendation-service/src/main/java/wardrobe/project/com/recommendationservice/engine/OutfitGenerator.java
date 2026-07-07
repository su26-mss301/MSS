package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OutfitGenerator {

    public List<ClothingItemExternalDTO> generateBestOutfit(List<ClothingItemExternalDTO> rankedItems) {
        if (rankedItems == null || rankedItems.isEmpty()) {
            return new ArrayList<>();
        }

        // Gom nhóm tất cả item theo ID danh mục (không cần biết ID đó là áo, quần hay giày)
        Map<UUID, List<ClothingItemExternalDTO>> groupedByCategory = rankedItems.stream()
                .filter(item -> item.getCategoryId() != null)
                .collect(Collectors.groupingBy(ClothingItemExternalDTO::getCategoryId));

        List<ClothingItemExternalDTO> finalOutfit = new ArrayList<>();

        // Quét qua các danh mục hiện có trong tủ đồ của người dùng
        for (List<ClothingItemExternalDTO> itemsInCategory : groupedByCategory.values()) {
            if (!itemsInCategory.isEmpty()) {
                // Lấy món đồ có điểm số cao nhất (index 0) của danh mục đó
                finalOutfit.add(itemsInCategory.get(0));
            }
        }

        // Giới hạn số lượng tối đa trong 1 set đồ để tránh kết hợp quá lố (Ví dụ: Tối đa 4 món)
        if (finalOutfit.size() > 4) {
            return new ArrayList<>(finalOutfit.subList(0, 4));
        }

        return finalOutfit;
    }
}