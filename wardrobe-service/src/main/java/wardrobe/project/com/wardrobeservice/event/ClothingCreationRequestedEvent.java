package wardrobe.project.com.wardrobeservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClothingCreationRequestedEvent {

    private String eventId;
    private String eventType;

    private Integer detectionLogId;
    private String userId;

    private String itemName;
    private String categoryId;
    private String zoneId;

    private String dominantColor;
    private String style;
    private Float confidenceScore;
    private String imageId;

    private String createdAt;
}