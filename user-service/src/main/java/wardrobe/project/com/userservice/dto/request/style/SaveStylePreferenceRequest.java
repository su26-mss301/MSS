package wardrobe.project.com.userservice.dto.request.style;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveStylePreferenceRequest {

    private List<String> favoriteColors;

    private List<String> preferredStyles;

    private List<String> lifestyles;

    private List<String> clothingInterests;
}