package wardrobe.project.com.userservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserStatusChangedEvent(
        UUID eventId,
        String userId,
        String previousStatus,
        String currentStatus,
        String changedBy,
        Instant occurredAt
) {
}