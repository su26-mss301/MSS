package wardrobe.project.com.recommendationservice.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ClothingItemDto {
    private UUID id;
    private String category; // Áo, Quần, Giày...
    private String color;
    private String style;    // Formal, Casual, Streetwear...
}