package wardrobe.project.com.wardrobeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeResponseDTO {
    private UUID wardrobeId;
    private UUID userId;
    private String wardrobeName;
    private LocalDateTime createdAt;
}
