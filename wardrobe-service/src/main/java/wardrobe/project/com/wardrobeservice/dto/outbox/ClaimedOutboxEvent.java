package wardrobe.project.com.wardrobeservice.dto.outbox;

public record ClaimedOutboxEvent(
        Long id,
        String eventId,
        String topic,
        String eventKey,
        String payload,
        Integer retryCount
) {
}