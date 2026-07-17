package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    /**
     * Lấy các event có thể publish và khóa các dòng đã lấy.
     *
     * SKIP LOCKED giúp instance khác bỏ qua những dòng
     * đang bị transaction hiện tại khóa.
     *
     * Phải gọi method này bên trong @Transactional.
     */
    @Query(
            value = """
                    SELECT *
                    FROM outbox_event
                    WHERE (
                        status = 'PENDING'
                        OR status = 'FAILED'
                    )
                    AND (
                        next_retry_at IS NULL
                        OR next_retry_at <= :now
                    )
                    ORDER BY created_at ASC, id ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findPublishableEventsForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    /**
     * Lấy các event đã bị kẹt ở PROCESSING.
     *
     * Ví dụ service đã claim event nhưng bị crash trước khi publish xong.
     */
    @Query(
            value = """
                    SELECT *
                    FROM outbox_event
                    WHERE status = 'PROCESSING'
                      AND processing_started_at IS NOT NULL
                      AND processing_started_at <= :expiredBefore
                    ORDER BY processing_started_at ASC, id ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findStuckProcessingEventsForUpdate(
            @Param("expiredBefore") LocalDateTime expiredBefore,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
            DELETE FROM OutboxEvent event
            WHERE event.status =
                wardrobe.project.com.wardrobeservice.entity.enums.OutboxStatus.PUBLISHED
              AND event.publishedAt < :expiredBefore
            """)
    int deletePublishedEventsBefore(
            @Param("expiredBefore") LocalDateTime expiredBefore
    );
}