package wardrobe.project.com.wardrobeservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import wardrobe.project.com.wardrobeservice.dto.outbox.ClaimedOutboxEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventService outboxEventService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Mỗi instance Wardrobe Service sẽ có một owner khác nhau.
     */
    private final String processingOwner =
            "wardrobe-service-" + UUID.randomUUID();

    @Value("${app.outbox.batch-size:20}")
    private int batchSize;

    @Value("${app.outbox.kafka-send-timeout-seconds:10}")
    private long kafkaSendTimeoutSeconds;

    /**
     * Method này không có @Transactional.
     *
     * Transaction claim event nằm trong OutboxEventService.
     * Khi gửi Kafka thì transaction database đã kết thúc.
     */
    public void publishPendingEvents() {
        List<ClaimedOutboxEvent> claimedEvents =
                outboxEventService.claimPublishableEvents(
                        batchSize,
                        processingOwner
                );

        if (claimedEvents.isEmpty()) {
            return;
        }

        log.info(
                "[OUTBOX] Instance {} đã claim {} event",
                processingOwner,
                claimedEvents.size()
        );

        for (ClaimedOutboxEvent event : claimedEvents) {
            publishSingleEvent(event);
        }
    }

    private void publishSingleEvent(
            ClaimedOutboxEvent event
    ) {
        try {
            /*
             * Không gửi trực tiếp payload String qua JsonSerializer.
             *
             * Nếu gửi String, JsonSerializer có thể biến payload thành:
             *
             * "{\"eventId\":\"...\"}"
             *
             * tức là một JSON string có dấu nháy bao ngoài.
             *
             * Vì vậy ta parse JSON string thành Object trước khi gửi.
             */
            Object kafkaPayload = parsePayload(event);

            SendResult<String, Object> result =
                    kafkaTemplate
                            .send(
                                    event.topic(),
                                    event.eventKey(),
                                    kafkaPayload
                            )
                            .get(
                                    kafkaSendTimeoutSeconds,
                                    TimeUnit.SECONDS
                            );

            /*
             * Chỉ đánh dấu PUBLISHED sau khi Kafka broker
             * xác nhận send thành công.
             */
            outboxEventService.markPublished(event.id());

            log.info(
                    "[OUTBOX] Publish thành công: " +
                            "outboxId={}, eventId={}, topic={}, partition={}, offset={}",
                    event.id(),
                    event.eventId(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );

        } catch (Exception exception) {
            String errorMessage = getRootErrorMessage(exception);

            try {
                outboxEventService.markFailed(
                        event.id(),
                        errorMessage
                );
            } catch (Exception updateException) {
                log.error(
                        "[OUTBOX] Không thể cập nhật FAILED: " +
                                "outboxId={}, eventId={}",
                        event.id(),
                        event.eventId(),
                        updateException
                );
            }

            log.error(
                    "[OUTBOX] Publish thất bại: " +
                            "outboxId={}, eventId={}, topic={}, retryCount={}, error={}",
                    event.id(),
                    event.eventId(),
                    event.topic(),
                    event.retryCount(),
                    errorMessage,
                    exception
            );
        }
    }

    private Object parsePayload(
            ClaimedOutboxEvent event
    ) {
        try {
            return objectMapper.readValue(
                    event.payload(),
                    Object.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Payload outbox không phải JSON hợp lệ: "
                            + "outboxId="
                            + event.id()
                            + ", eventId="
                            + event.eventId(),
                    exception
            );
        }
    }

    private String getRootErrorMessage(
            Throwable throwable
    ) {
        Throwable rootCause = throwable;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String message = rootCause.getMessage();

        if (message == null || message.isBlank()) {
            return rootCause.getClass().getName();
        }

        return rootCause.getClass().getSimpleName()
                + ": "
                + message;
    }
}