package wardrobe.project.com.wardrobeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeUpdateRequestDTO {

    @NotBlank(message = "WARDROBE_NAME_BLANK")
    @Size(max = 100, message = "WARDROBE_NAME_SIZE")
    private String wardrobeName;
}
