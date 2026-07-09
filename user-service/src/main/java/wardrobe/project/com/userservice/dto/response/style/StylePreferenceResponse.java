package wardrobe.project.com.userservice.dto.response.style;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylePreferenceResponse {

    private String preferenceId;

    private List<String> favoriteColors;

    private List<String> preferredStyles;

    private List<String> lifestyles;

    private List<String> clothingInterests;

    private LocalDateTime updatedAt;
}