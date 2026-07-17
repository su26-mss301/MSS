package wardrobe.project.com.wardrobeservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.ClothingItemUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.ClothingItemResponseDTO;
import wardrobe.project.com.wardrobeservice.entity.*;
import wardrobe.project.com.wardrobeservice.entity.enums.OutboxStatus;
import wardrobe.project.com.wardrobeservice.event.ClothingCreatedEvent;
import wardrobe.project.com.wardrobeservice.event.ClothingCreationRequestedEvent;
import wardrobe.project.com.wardrobeservice.exception.AppException;
import wardrobe.project.com.wardrobeservice.exception.ErrorCode;
import wardrobe.project.com.wardrobeservice.repository.*;
import wardrobe.project.com.wardrobeservice.service.ClothingItemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothingItemServiceImpl implements ClothingItemService {

    private final ClothingItemRepository clothingItemRepository;
    private final WardrobeZoneRepository wardrobeZoneRepository;
    private final CategoryRepository categoryRepository;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.clothing-created}")
    private String clothingCreatedTopic;
    @Override
    public ClothingItemResponseDTO createClothingItem(ClothingItemCreateRequestDTO request) {
        WardrobeZone zone = null;
        if (request.getZoneId() != null) {
            zone = wardrobeZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        ClothingItem item = ClothingItem.builder()
                .zone(zone)
                .category(category)
                .imageId(request.getImageId())
                .itemName(request.getItemName())
                .dominantColor(request.getDominantColor())
                .style(request.getStyle())
                .confidenceScore(request.getConfidenceScore())
                .build();
        
        ClothingItem savedItem = clothingItemRepository.save(item);
        return mapToResponse(savedItem);
    }

    @Override
    public ClothingItemResponseDTO getClothingItemById(UUID id) {
        ClothingItem item = clothingItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND));
        return mapToResponse(item);
    }

    @Override
    public List<ClothingItemResponseDTO> getAllClothingItems(UUID userId) {
        return clothingItemRepository.findByZone_Wardrobe_UserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClothingItemResponseDTO> getItemsByZoneId(UUID zoneId) {
        return clothingItemRepository.findByZone_ZoneId(zoneId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClothingItemResponseDTO> getItemsByCategoryId(UUID categoryId) {
        return clothingItemRepository.findByCategory_CategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClothingItemResponseDTO updateClothingItem(UUID id, ClothingItemUpdateRequestDTO request) {
        ClothingItem item = clothingItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND));

        if (request.getZoneId() != null) {
            WardrobeZone zone = wardrobeZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new AppException(ErrorCode.WARDROBE_ZONE_NOT_FOUND));
            item.setZone(zone);
        } else {
            item.setZone(null);
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            item.setCategory(category);
        } else {
            item.setCategory(null);
        }

        item.setImageId(request.getImageId());
        item.setItemName(request.getItemName());
        item.setDominantColor(request.getDominantColor());
        item.setStyle(request.getStyle());
        item.setConfidenceScore(request.getConfidenceScore());

        ClothingItem updatedItem = clothingItemRepository.save(item);
        return mapToResponse(updatedItem);
    }

    @Override
    public void deleteClothingItem(UUID id) {
        if (!clothingItemRepository.existsById(id)) {
            throw new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND);
        }
        clothingItemRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ClothingItemResponseDTO createClothingItemFromEvent(
            ClothingCreationRequestedEvent event
    ) {
        validateCreationRequestedEvent(event);

        /*
         * Kiểm tra nhanh để tránh làm lại công việc khi event chắc chắn đã xử lý.
         *
         * Tuy nhiên, kiểm tra này không đủ để chống race condition.
         * Unique constraint và saveAndFlush phía dưới mới là lớp bảo vệ chính.
         */
        if (processedKafkaEventRepository.existsByEventId(event.getEventId())) {
            log.info(
                    "[KAFKA] Event đã được xử lý, bỏ qua: {}",
                    event.getEventId()
            );
            return null;
        }

        /*
         * Ghi eventId trước và flush ngay.
         *
         * Nếu hai instance xử lý cùng eventId:
         * - một instance insert thành công;
         * - instance còn lại bị unique constraint;
         * - transaction bị rollback trước khi ClothingItem được tạo.
         */
        ProcessedKafkaEvent processedEvent =
                ProcessedKafkaEvent.builder()
                        .eventId(event.getEventId())
                        .eventType(event.getEventType())
                        .processedAt(LocalDateTime.now())
                        .build();

        try {
            processedKafkaEventRepository.saveAndFlush(processedEvent);
        } catch (DataIntegrityViolationException exception) {
            log.info(
                    "[KAFKA] Event đã được instance khác xử lý, bỏ qua: {}",
                    event.getEventId()
            );

            /*
             * Không nên tiếp tục sử dụng transaction sau lỗi SQL.
             * Throw exception để transaction hiện tại rollback hoàn toàn.
             *
             * Listener cần xử lý riêng duplicate ở bước sau nếu muốn không retry.
             */
            throw exception;
        }

        WardrobeZone zone = null;

        if (event.getZoneId() != null
                && !event.getZoneId().isBlank()) {
            UUID zoneId = parseUuid(
                    event.getZoneId(),
                    "zoneId",
                    event.getEventId()
            );

            zone = wardrobeZoneRepository.findById(zoneId)
                    .orElseThrow(() ->
                            new AppException(
                                    ErrorCode.WARDROBE_ZONE_NOT_FOUND
                            )
                    );
        }

        Category category = null;

        if (event.getCategoryId() != null
                && !event.getCategoryId().isBlank()) {
            UUID categoryId = parseUuid(
                    event.getCategoryId(),
                    "categoryId",
                    event.getEventId()
            );

            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() ->
                            new AppException(
                                    ErrorCode.CATEGORY_NOT_FOUND
                            )
                    );
        }

        UUID imageId = null;

        if (event.getImageId() != null
                && !event.getImageId().isBlank()) {
            imageId = parseUuid(
                    event.getImageId(),
                    "imageId",
                    event.getEventId()
            );
        }

        ClothingItem clothingItem =
                ClothingItem.builder()
                        .zone(zone)
                        .category(category)
                        .imageId(imageId)
                        .itemName(event.getItemName())
                        .dominantColor(event.getDominantColor())
                        .style(event.getStyle())
                        .confidenceScore(event.getConfidenceScore())
                        .build();

        ClothingItem savedItem =
                clothingItemRepository.save(clothingItem);

        /*
         * Cần flush để UUID itemId chắc chắn có trước khi tạo payload outbox.
         * Với GenerationType.UUID thường ID đã có ngay, nhưng flush giúp
         * lỗi database được phát hiện trong transaction hiện tại.
         */
        clothingItemRepository.flush();

        LocalDateTime now = LocalDateTime.now();
        String resultEventId = UUID.randomUUID().toString();

        ClothingCreatedEvent resultEvent =
                ClothingCreatedEvent.builder()
                        .eventId(resultEventId)
                        .eventType("CLOTHING_CREATED")
                        .requestEventId(event.getEventId())
                        .detectionLogId(event.getDetectionLogId())
                        .userId(event.getUserId())
                        .clothingItemId(
                                savedItem.getItemId().toString()
                        )
                        .imageId(event.getImageId())
                        .itemName(event.getItemName())
                        .createdAt(now)
                        .build();

        String payload = serializeEvent(resultEvent);

        OutboxEvent outboxEvent =
                OutboxEvent.builder()
                        .eventId(resultEventId)
                        .aggregateType("CLOTHING_ITEM")
                        .aggregateId(
                                savedItem.getItemId().toString()
                        )
                        .eventType("CLOTHING_CREATED")
                        .topic(clothingCreatedTopic)
                        .eventKey(event.getEventId())
                        .payload(payload)
                        .status(OutboxStatus.PENDING)
                        .retryCount(0)
                        .createdAt(now)
                        .nextRetryAt(now)
                        .build();

        outboxEventRepository.save(outboxEvent);

        log.info(
                "[OUTBOX] Đã lưu ClothingCreatedEvent vào outbox: " +
                        "requestEventId={}, outboxEventId={}, clothingItemId={}",
                event.getEventId(),
                resultEventId,
                savedItem.getItemId()
        );

        return mapToResponse(savedItem);
    }

    private void validateCreationRequestedEvent(
            ClothingCreationRequestedEvent event
    ) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "ClothingCreationRequestedEvent không được null"
            );
        }

        if (event.getEventId() == null
                || event.getEventId().isBlank()) {
            throw new IllegalArgumentException(
                    "eventId không được để trống"
            );
        }

        if (event.getEventType() == null
                || event.getEventType().isBlank()) {
            throw new IllegalArgumentException(
                    "eventType không được để trống"
            );
        }

        if (event.getItemName() == null
                || event.getItemName().isBlank()) {
            throw new IllegalArgumentException(
                    "itemName không được để trống"
            );
        }
    }

    private UUID parseUuid(
            String value,
            String fieldName,
            String eventId
    ) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Giá trị UUID không hợp lệ: field="
                            + fieldName
                            + ", value="
                            + value
                            + ", eventId="
                            + eventId,
                    exception
            );
        }
    }

    private String serializeEvent(
            ClothingCreatedEvent event
    ) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Không thể serialize ClothingCreatedEvent: eventId="
                            + event.getEventId(),
                    exception
            );
        }
    }

    private ClothingItemResponseDTO mapToResponse(ClothingItem item) {
        return ClothingItemResponseDTO.builder()
                .itemId(item.getItemId())
                .zoneId(item.getZone() != null ? item.getZone().getZoneId() : null)
                .categoryId(item.getCategory() != null ? item.getCategory().getCategoryId() : null)
                .imageId(item.getImageId())
                .itemName(item.getItemName())
                .dominantColor(item.getDominantColor())
                .style(item.getStyle())
                .confidenceScore(item.getConfidenceScore())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
