package wardrobe.project.com.userservice.config.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.entity.OutboxEvent;
import wardrobe.project.com.userservice.enums.OutboxStatus;
import wardrobe.project.com.userservice.event.UserStatusChangedEvent;
import wardrobe.project.com.userservice.kafka.KafkaTopics;
import wardrobe.project.com.userservice.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, UserStatusChangedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop50ByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING
                        );

        for (OutboxEvent outboxEvent : events) {
            publish(outboxEvent);
        }
    }

    private void publish(OutboxEvent outboxEvent) {
        try {
            UserStatusChangedEvent event =
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            UserStatusChangedEvent.class
                    );

            kafkaTemplate.send(
                    KafkaTopics.USER_STATUS_CHANGED,
                    event.userId(),
                    event
            ).get();

            outboxEvent.setStatus(OutboxStatus.SENT);
            outboxEvent.setSentAt(LocalDateTime.now());
            outboxEvent.setLastError(null);

        } catch (Exception exception) {
            int retryCount = outboxEvent.getRetryCount() + 1;

            outboxEvent.setRetryCount(retryCount);
            outboxEvent.setLastError(exception.getMessage());

            if (retryCount >= 100) {
                outboxEvent.setStatus(OutboxStatus.FAILED);
            } else {
                outboxEvent.setStatus(OutboxStatus.PENDING);
            }

            log.error(
                    "Cannot publish outbox event: eventId={}, retryCount={}",
                    outboxEvent.getEventId(),
                    retryCount,
                    exception
            );
        }

        outboxEventRepository.save(outboxEvent);
    }
}