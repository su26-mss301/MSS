package wardrobe.project.com.wardrobeservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.wardrobeservice.repository.OutboxEventRepository;
import wardrobe.project.com.wardrobeservice.service.OutboxEventService;
import wardrobe.project.com.wardrobeservice.service.OutboxPublisher;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxPublisher outboxPublisher;
    private final OutboxEventService outboxEventService;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${app.outbox.recovery-batch-size:100}")
    private int recoveryBatchSize;

    @Value("${app.outbox.processing-timeout-seconds:300}")
    private long processingTimeoutSeconds;

    @Scheduled(
            fixedDelayString =
                    "${app.outbox.publisher-fixed-delay-ms:3000}"
    )
    public void publishOutboxEvents() {
        try {
            outboxPublisher.publishPendingEvents();
        } catch (Exception exception) {
            log.error(
                    "[OUTBOX] Lỗi khi chạy Outbox Publisher",
                    exception
            );
        }
    }

    @Scheduled(
            fixedDelayString =
                    "${app.outbox.recovery-fixed-delay-ms:60000}",
            initialDelayString =
                    "${app.outbox.recovery-initial-delay-ms:30000}"
    )
    public void recoverStuckOutboxEvents() {
        try {
            int recoveredCount =
                    outboxEventService.recoverStuckEvents(
                            recoveryBatchSize,
                            processingTimeoutSeconds
                    );

            if (recoveredCount > 0) {
                log.warn(
                        "[OUTBOX] Đã phục hồi {} event bị kẹt",
                        recoveredCount
                );
            }
        } catch (Exception exception) {
            log.error(
                    "[OUTBOX] Lỗi khi phục hồi event PROCESSING",
                    exception
            );
        }
    }

    @Value("${app.outbox.retention-days:30}")
    private long retentionDays;

    @Scheduled(
            cron = "${app.outbox.cleanup-cron:0 0 2 * * *}"
    )
    @Transactional
    public void cleanupPublishedOutboxEvents() {
        LocalDateTime expiredBefore =
                LocalDateTime.now().minusDays(retentionDays);

        int deletedCount =
                outboxEventRepository
                        .deletePublishedEventsBefore(expiredBefore);

        if (deletedCount > 0) {
            log.info(
                    "[OUTBOX] Đã xóa {} event PUBLISHED quá {} ngày",
                    deletedCount,
                    retentionDays
            );
        }
    }
}