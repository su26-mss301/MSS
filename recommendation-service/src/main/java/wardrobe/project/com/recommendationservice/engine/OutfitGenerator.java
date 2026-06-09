package wardrobe.project.com.recommendationservice.engine;

import org.springframework.stereotype.Component;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;
import java.util.List;
import java.util.Optional;

@Component
public class OutfitGenerator {

    public List<ClothingItemExternalDTO> generateBestOutfit(List<ClothingItemExternalDTO> topRankedItems) {
        Optional<ClothingItemExternalDTO> bestTop = topRankedItems.stream()
                .filter(item -> item.getItemName() != null &&
                        (item.getItemName().toLowerCase().contains("shirt") || item.getItemName().toLowerCase().contains("áo")))
                .findFirst();

        Optional<ClothingItemExternalDTO> bestBottom = topRankedItems.stream()
                .filter(item -> item.getItemName() != null &&
                        (item.getItemName().toLowerCase().contains("pants") || item.getItemName().toLowerCase().contains("quần")))
                .findFirst();

        Optional<ClothingItemExternalDTO> bestShoes = topRankedItems.stream()
                .filter(item -> item.getItemName() != null &&
                        (item.getItemName().toLowerCase().contains("shoes") || item.getItemName().toLowerCase().contains("giày")))
                .findFirst();

        if (bestTop.isPresent() && bestBottom.isPresent()) {
            return List.of(bestTop.get(), bestBottom.get(), bestShoes.orElse(null));
        }
        return List.of();
    }
}