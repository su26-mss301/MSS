package wardrobe.project.com.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import wardrobe.project.com.userservice.kafka.KafkaTopics;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userStatusChangedTopic() {
        return TopicBuilder
                .name(KafkaTopics.USER_STATUS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}