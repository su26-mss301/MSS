package wardrobe.project.com.userservice.service.cleanup.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.enums.OutboxStatus;
import wardrobe.project.com.userservice.repository.OutboxEventRepository;
import wardrobe.project.com.userservice.service.cleanup.OutboxCleanupService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxCleanupServiceImpl
        implements OutboxCleanupService {

    private final OutboxEventRepository outboxEventRepository;

    @Value("${app.outbox.retention-days:30}")
    private long retentionDays;

    @Override
    @Transactional
    public int cleanupSentEvents() {
        LocalDateTime expiredBefore =
                LocalDateTime.now().minusDays(retentionDays);

        int deletedCount =
                outboxEventRepository.deleteSentEventsBefore(
                        OutboxStatus.SENT,
                        expiredBefore
                );

        if (deletedCount > 0) {
            log.info(
                    "[USER-OUTBOX] Đã cleanup {} event SENT quá {} ngày",
                    deletedCount,
                    retentionDays
            );
        }

        return deletedCount;
    }
}