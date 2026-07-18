package wardrobe.project.com.wardrobeservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import wardrobe.project.com.wardrobeservice.dto.response.ClothingItemResponseDTO;
import wardrobe.project.com.wardrobeservice.event.ClothingCreationRequestedEvent;
import wardrobe.project.com.wardrobeservice.service.ClothingItemService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothingCreationRequestedConsumer {

    private final ClothingItemService clothingItemService;

    @KafkaListener(
            topics = "${app.kafka.topic.clothing-creation-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(
            ClothingCreationRequestedEvent event
    ) {
        log.info(
                "[KAFKA] Nhận yêu cầu tạo ClothingItem: eventId={}, userId={}, itemName={}",
                event.getEventId(),
                event.getUserId(),
                event.getItemName()
        );

        ClothingItemResponseDTO result =
                clothingItemService.createClothingItemFromEvent(event);

        if (result == null) {
            log.info(
                    "[KAFKA] Bỏ qua event đã xử lý: {}",
                    event.getEventId()
            );
            return;
        }

        log.info(
                "[KAFKA] Tạo ClothingItem thành công: eventId={}, itemId={}",
                event.getEventId(),
                result.getItemId()
        );
    }
}