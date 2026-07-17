package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.wardrobeservice.dto.outbox.ClaimedOutboxEvent;
import wardrobe.project.com.wardrobeservice.entity.OutboxEvent;
import wardrobe.project.com.wardrobeservice.entity.enums.OutboxStatus;
import wardrobe.project.com.wardrobeservice.repository.OutboxEventRepository;
import wardrobe.project.com.wardrobeservice.service.OutboxEventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventServiceImpl implements OutboxEventService {

    private static final int MAX_ERROR_LENGTH = 4000;

    private final OutboxEventRepository outboxEventRepository;

    /**
     * Transaction ngắn:
     *
     * 1. Khóa batch bằng FOR UPDATE SKIP LOCKED.
     * 2. Chuyển các event sang PROCESSING.
     * 3. Commit.
     * 4. Trả dữ liệu cần gửi cho publisher.
     */
    @Override
    @Transactional
    public List<ClaimedOutboxEvent> claimPublishableEvents(
            int batchSize,
            String processingOwner
    ) {
        LocalDateTime now = LocalDateTime.now();

        List<OutboxEvent> events =
                outboxEventRepository.findPublishableEventsForUpdate(
                        now,
                        batchSize
                );

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setProcessingStartedAt(now);
            event.setProcessingOwner(processingOwner);
        }

        /*
         * Không bắt buộc gọi saveAll vì các entity đang managed.
         * Hibernate sẽ dirty-check khi transaction commit.
         */
        return events.stream()
                .map(event -> new ClaimedOutboxEvent(
                        event.getId(),
                        event.getEventId(),
                        event.getTopic(),
                        event.getEventKey(),
                        event.getPayload(),
                        event.getRetryCount()
                ))
                .toList();
    }

    /**
     * Dùng transaction riêng vì method này được gọi
     * sau khi Kafka broker đã xác nhận gửi thành công.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(Long outboxEventId) {
        OutboxEvent event = outboxEventRepository
                .findById(outboxEventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy OutboxEvent id=" + outboxEventId
                ));

        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            log.info(
                    "[OUTBOX] Event đã ở trạng thái PUBLISHED: id={}, eventId={}",
                    event.getId(),
                    event.getEventId()
            );
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setLastError(null);
        event.setNextRetryAt(null);
        event.setProcessingStartedAt(null);
        event.setProcessingOwner(null);

        log.info(
                "[OUTBOX] Đánh dấu PUBLISHED: id={}, eventId={}",
                event.getId(),
                event.getEventId()
        );
    }

    /**
     * Kafka gửi thất bại:
     *
     * - tăng retryCount;
     * - lưu lỗi;
     * - tính thời điểm retry tiếp theo;
     * - chuyển PROCESSING → FAILED.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            Long outboxEventId,
            String errorMessage
    ) {
        OutboxEvent event = outboxEventRepository
                .findById(outboxEventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy OutboxEvent id=" + outboxEventId
                ));

        if (event.getStatus() == OutboxStatus.PUBLISHED) {
            log.warn(
                    "[OUTBOX] Không chuyển PUBLISHED thành FAILED: id={}, eventId={}",
                    event.getId(),
                    event.getEventId()
            );
            return;
        }

        int currentRetryCount =
                event.getRetryCount() == null
                        ? 0
                        : event.getRetryCount();

        int nextRetryCount = currentRetryCount + 1;
        long delaySeconds = calculateBackoffSeconds(nextRetryCount);

        event.setStatus(OutboxStatus.FAILED);
        event.setRetryCount(nextRetryCount);
        event.setLastError(truncateError(errorMessage));
        event.setNextRetryAt(
                LocalDateTime.now().plusSeconds(delaySeconds)
        );
        event.setProcessingStartedAt(null);
        event.setProcessingOwner(null);

        log.warn(
                "[OUTBOX] Publish thất bại: id={}, eventId={}, retryCount={}, retrySau={} giây",
                event.getId(),
                event.getEventId(),
                nextRetryCount,
                delaySeconds
        );
    }

    /**
     * Phục hồi những event bị kẹt ở PROCESSING do service crash.
     */
    @Override
    @Transactional
    public int recoverStuckEvents(
            int batchSize,
            long processingTimeoutSeconds
    ) {
        LocalDateTime expiredBefore =
                LocalDateTime.now()
                        .minusSeconds(processingTimeoutSeconds);

        List<OutboxEvent> stuckEvents =
                outboxEventRepository.findStuckProcessingEventsForUpdate(
                        expiredBefore,
                        batchSize
                );

        LocalDateTime now = LocalDateTime.now();

        for (OutboxEvent event : stuckEvents) {
            event.setStatus(OutboxStatus.FAILED);
            event.setLastError(
                    "Event được phục hồi vì bị kẹt ở PROCESSING"
            );
            event.setNextRetryAt(now);
            event.setProcessingStartedAt(null);
            event.setProcessingOwner(null);
        }

        if (!stuckEvents.isEmpty()) {
            log.warn(
                    "[OUTBOX] Đã phục hồi {} event bị kẹt ở PROCESSING",
                    stuckEvents.size()
            );
        }

        return stuckEvents.size();
    }

    /**
     * Exponential backoff:
     *
     * retry 1: 2 giây
     * retry 2: 4 giây
     * retry 3: 8 giây
     * retry 4: 16 giây
     * ...
     * tối đa 5 phút.
     */
    private long calculateBackoffSeconds(int retryCount) {
        int safeRetryCount = Math.min(retryCount, 20);

        long delay = 1L << safeRetryCount;

        return Math.min(delay, 300L);
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown Kafka publish error";
        }

        if (errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }

        return errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}