package wardrobe.project.com.wardrobeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingItemUpdateRequestDTO {

    private UUID zoneId;
    private UUID categoryId;
    private UUID imageId;

    @NotBlank(message = "ITEM_NAME_BLANK")
    @Size(max = 100, message = "ITEM_NAME_SIZE")
    private String itemName;

    @Size(max = 50, message = "COLOR_SIZE")
    private String dominantColor;

    @Size(max = 50, message = "STYLE_SIZE")
    private String style;

    private Float confidenceScore;
}
