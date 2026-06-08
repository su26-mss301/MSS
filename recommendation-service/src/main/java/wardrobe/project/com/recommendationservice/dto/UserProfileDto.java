package wardrobe.project.com.recommendationservice.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UserProfileDto {
    private UUID id;
    private List<String> favoriteColors;
    private String preferredStyle;
}