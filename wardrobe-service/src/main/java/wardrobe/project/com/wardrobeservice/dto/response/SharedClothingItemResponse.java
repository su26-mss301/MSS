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
public class SharedClothingItemResponse {

    private UUID shareId;

    // Thông tin item được share
    private UUID itemId;
    private String itemName;
    private UUID imageId;
    private String dominantColor;
    private String style;
    private Float confidenceScore;

    // Thông tin share
    private String groupId;
    private UUID sharedByUserId;
    private LocalDateTime sharedAt;

    // Like
    private long likeCount;
    private boolean likedByMe;  // true nếu currentUser đã like
}
