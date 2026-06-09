package wardrobe.project.com.recommendationservice.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileExternalDTO {
    private UUID id;
    private List<String> favoriteColors;
    private String preferredStyle;
}