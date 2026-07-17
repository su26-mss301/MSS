package wardrobe.project.com.storageservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClothingCreatedEvent {

    private String eventId;
    private String eventType;
    private String requestEventId;

    private Integer detectionLogId;
    private String userId;

    private String clothingItemId;
    private String imageId;
    private String itemName;

    private LocalDateTime createdAt;
}