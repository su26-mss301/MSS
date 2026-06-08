package wardrobe.project.com.wardrobeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class WardrobeZoneUpdateRequestDTO {

    @NotNull(message = "WARDROBE_ID_BLANK")
    private UUID wardrobeId;

    @NotBlank(message = "ZONE_NAME_BLANK")
    @Size(max = 100, message = "ZONE_NAME_SIZE")
    private String zoneName;

    private String description;
}
