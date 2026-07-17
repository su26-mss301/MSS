package wardrobe.project.com.storageservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import wardrobe.project.com.storageservice.event.ClothingCreatedEvent;
import wardrobe.project.com.storageservice.service.StorageService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothingCreatedConsumer {

    private final StorageService storageService;

    @KafkaListener(
            topics = "${app.kafka.topic.clothing-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ClothingCreatedEvent event) {
        log.info(
                "[KAFKA] Storage nhận ClothingCreatedEvent: " +
                        "eventId={}, imageId={}, clothingItemId={}",
                event.getEventId(),
                event.getImageId(),
                event.getClothingItemId()
        );

        if (
                event.getImageId() == null
                || event.getImageId().isBlank()
        ) {
            log.warn(
                    "[KAFKA] Event không có imageId, bỏ qua confirm ảnh: {}",
                    event.getEventId()
            );
            return;
        }

        storageService.confirmImageFromEvent(
                event.getImageId()
        );

        log.info(
                "[KAFKA] Ảnh đã chuyển thành DONE: imageId={}",
                event.getImageId()
        );
    }
}