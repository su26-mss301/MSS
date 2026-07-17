package wardrobe.project.com.wardrobeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import wardrobe.project.com.wardrobeservice.entity.enums.OutboxStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(
                        name = "idx_outbox_event_status_next_retry",
                        columnList = "status,next_retry_at"
                ),
                @Index(
                        name = "idx_outbox_event_created_at",
                        columnList = "created_at"
                ),
                @Index(
                        name = "idx_outbox_event_aggregate",
                        columnList = "aggregate_type,aggregate_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_outbox_event_event_id",
                        columnNames = "event_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID nghiệp vụ của event được gửi sang Kafka.
     *
     * Ví dụ:
     * ClothingCreatedEvent.eventId
     */
    @Column(
            name = "event_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String eventId;

    /**
     * Loại aggregate phát sinh event.
     *
     * Ví dụ:
     * CLOTHING_ITEM
     */
    @Column(
            name = "aggregate_type",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String aggregateType;

    /**
     * ID của aggregate.
     *
     * Với ClothingCreatedEvent, đây là clothingItemId.
     */
    @Column(
            name = "aggregate_id",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String aggregateId;

    /**
     * Loại event.
     *
     * Ví dụ:
     * CLOTHING_CREATED
     */
    @Column(
            name = "event_type",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String eventType;

    /**
     * Kafka topic cần publish.
     */
    @Column(
            name = "topic",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String topic;

    /**
     * Kafka message key.
     *
     * Theo producer hiện tại của bạn,
     * key sẽ là requestEventId.
     */
    @Column(
            name = "event_key",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String eventKey;

    /**
     * Payload JSON của ClothingCreatedEvent.
     *
     * Sử dụng columnDefinition = "TEXT" để Hibernate tạo kiểu TEXT.
     * Migration SQL phía dưới sẽ dùng JSONB phù hợp với PostgreSQL.
     */
    @Column(
            name = "payload",
            nullable = false,
            updatable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private OutboxStatus status;

    @Column(
            name = "retry_count",
            nullable = false
    )
    private Integer retryCount;

    /**
     * Lỗi gần nhất khi gửi Kafka.
     *
     * Dùng TEXT vì stack trace hoặc error message có thể dài.
     */
    @Column(
            name = "last_error",
            columnDefinition = "TEXT"
    )
    private String lastError;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Thời điểm event được phép retry tiếp.
     *
     * PENDING mới tạo có thể đặt bằng createdAt.
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Thời điểm instance publisher nhận event để xử lý.
     *
     * Field này không nằm trong danh sách ban đầu của bạn,
     * nhưng rất cần để phục hồi event bị kẹt ở PROCESSING
     * khi service bị crash.
     */
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    /**
     * ID instance đang xử lý event.
     *
     * Ví dụ:
     * wardrobe-service-1
     */
    @Column(
            name = "processing_owner",
            length = 255
    )
    private String processingOwner;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }

        if (status == null) {
            status = OutboxStatus.PENDING;
        }

        if (retryCount == null) {
            retryCount = 0;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (nextRetryAt == null) {
            nextRetryAt = now;
        }
    }
}