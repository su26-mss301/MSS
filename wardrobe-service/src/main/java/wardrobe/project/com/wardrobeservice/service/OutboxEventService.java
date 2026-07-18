package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.outbox.ClaimedOutboxEvent;

import java.util.List;

public interface OutboxEventService {

    List<ClaimedOutboxEvent> claimPublishableEvents(
            int batchSize,
            String processingOwner
    );

    void markPublished(Long outboxEventId);

    void markFailed(
            Long outboxEventId,
            String errorMessage
    );

    int recoverStuckEvents(
            int batchSize,
            long processingTimeoutSeconds
    );
}