package wardrobe.project.com.recommendationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wardrobe.project.com.recommendationservice.dto.external.ClothingItemExternalDTO;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitResponseDTO {
    private UUID outfitId;
    private String outfitName;
    private String description;
    private String img;
    private int items;
    private List<String> tags;
    private List<ClothingItemExternalDTO> clothingItems;
}