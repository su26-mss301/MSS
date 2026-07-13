package wardrobe.project.com.userservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.event.UserStatusChangedEvent;

@Component
@RequiredArgsConstructor
public class UserStatusEventProducer {

    private final KafkaTemplate<String, UserStatusChangedEvent> kafkaTemplate;

    public void publish(UserStatusChangedEvent event) {
        kafkaTemplate.send(
                KafkaTopics.USER_STATUS_CHANGED,
                event.userId(),
                event
        );
    }
}