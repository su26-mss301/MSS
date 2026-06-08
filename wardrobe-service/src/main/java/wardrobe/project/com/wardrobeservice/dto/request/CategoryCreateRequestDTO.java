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
public class CategoryCreateRequestDTO {

    @NotBlank(message = "CATEGORY_NAME_BLANK")
    @Size(max = 100, message = "CATEGORY_NAME_SIZE")
    private String categoryName;

    private String description;
}
