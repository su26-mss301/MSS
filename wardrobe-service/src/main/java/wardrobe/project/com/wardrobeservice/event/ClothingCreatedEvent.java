package wardrobe.project.com.wardrobeservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingCreatedEvent {

    private String eventId;
    private String eventType;

    // Event yêu cầu ban đầu
    private String requestEventId;

    private Integer detectionLogId;
    private String userId;

    private String clothingItemId;
    private String imageId;
    private String itemName;

    private LocalDateTime createdAt;
}