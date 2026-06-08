package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.ClothingItemDto;
import java.util.List;
import java.util.Optional;

@Component
public class OutfitGenerator {

    // Hàm nhận vào một list đồ đã được chấm điểm (từ cao xuống thấp) và ghép thành Outfit
    public List<ClothingItemDto> generateBestOutfit(List<ClothingItemDto> topRankedItems) {
        // Tìm cái áo (Top) điểm cao nhất
        Optional<ClothingItemDto> bestTop = topRankedItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase("Top") || item.getCategory().equalsIgnoreCase("Shirt"))
                .findFirst();

        // Tìm cái quần (Bottom) điểm cao nhất
        Optional<ClothingItemDto> bestBottom = topRankedItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase("Bottom") || item.getCategory().equalsIgnoreCase("Trousers"))
                .findFirst();

        // Tìm đôi giày (Shoes) điểm cao nhất
        Optional<ClothingItemDto> bestShoes = topRankedItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase("Shoes"))
                .findFirst();

        // Nếu có đủ áo và quần thì mới coi là một bộ outfit hợp lệ
        if (bestTop.isPresent() && bestBottom.isPresent()) {
            return List.of(
                    bestTop.get(),
                    bestBottom.get(),
                    bestShoes.orElse(null) // Giày có thể null nếu trong tủ không có
            );
        }

        return List.of(); // Trả về list rỗng nếu không thể phối thành bộ
    }
}