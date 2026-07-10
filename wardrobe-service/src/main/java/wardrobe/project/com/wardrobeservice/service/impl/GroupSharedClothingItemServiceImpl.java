package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.wardrobeservice.dto.request.ShareClothingItemRequest;
import wardrobe.project.com.wardrobeservice.dto.response.SharedClothingItemResponse;
import wardrobe.project.com.wardrobeservice.entity.ClothingItem;
import wardrobe.project.com.wardrobeservice.entity.GroupSharedClothingItem;
import wardrobe.project.com.wardrobeservice.entity.SharedClothingItemLike;
import wardrobe.project.com.wardrobeservice.exception.AppException;
import wardrobe.project.com.wardrobeservice.exception.ErrorCode;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.repository.GroupSharedClothingItemRepository;
import wardrobe.project.com.wardrobeservice.repository.SharedClothingItemLikeRepository;
import wardrobe.project.com.wardrobeservice.service.GroupSharedClothingItemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupSharedClothingItemServiceImpl implements GroupSharedClothingItemService {

    private final GroupSharedClothingItemRepository groupSharedRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final SharedClothingItemLikeRepository likeRepository;

    // ─── Share ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SharedClothingItemResponse shareItem(UUID userId, ShareClothingItemRequest request) {
        ClothingItem item = clothingItemRepository.findById(request.getClothingItemId())
                .orElseThrow(() -> new AppException(ErrorCode.CLOTHING_ITEM_NOT_FOUND));

        Optional<GroupSharedClothingItem> existing =
                groupSharedRepository.findActiveByItemIdAndGroupId(item.getItemId(), request.getGroupId());
        if (existing.isPresent()) {
            return mapToResponse(existing.get(), userId);
        }

        GroupSharedClothingItem record = GroupSharedClothingItem.builder()
                .clothingItem(item)
                .groupId(request.getGroupId())
                .sharedByUserId(userId)
                .build();

        return mapToResponse(groupSharedRepository.save(record), userId);
    }

    // ─── Get List ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SharedClothingItemResponse> getSharedItemsByGroup(String groupId, UUID currentUserId) {
        List<GroupSharedClothingItem> records = groupSharedRepository.findActiveByGroupId(groupId);

        // Batch fetch các shareId mà currentUser đã like (tránh N+1)
        List<UUID> shareIds = records.stream()
                .map(GroupSharedClothingItem::getShareId)
                .collect(Collectors.toList());

        Set<UUID> likedSet = currentUserId != null && !shareIds.isEmpty()
                ? Set.copyOf(likeRepository.findLikedShareIdsByUserIdAndShareIdIn(shareIds, currentUserId))
                : Set.of();

        return records.stream()
                .map(r -> mapToResponseWithLikeSet(r, likedSet))
                .collect(Collectors.toList());
    }

    // ─── Unshare ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void unshareItem(UUID shareId, UUID userId) {
        GroupSharedClothingItem record = groupSharedRepository.findById(shareId)
                .orElseThrow(() -> new AppException(ErrorCode.SHARED_ITEM_NOT_FOUND));

        if (record.getDeletedAt() != null) {
            throw new AppException(ErrorCode.SHARED_ITEM_NOT_FOUND);
        }

        if (!record.getSharedByUserId().equals(userId)) {
            throw new AppException(ErrorCode.SHARED_ITEM_FORBIDDEN);
        }

        record.setDeletedAt(LocalDateTime.now());
        groupSharedRepository.save(record);
    }

    // ─── Toggle Like ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean toggleLike(UUID shareId, UUID userId) {
        GroupSharedClothingItem record = groupSharedRepository.findById(shareId)
                .orElseThrow(() -> new AppException(ErrorCode.SHARED_ITEM_NOT_FOUND));

        if (record.getDeletedAt() != null) {
            throw new AppException(ErrorCode.SHARED_ITEM_NOT_FOUND);
        }

        Optional<SharedClothingItemLike> existing =
                likeRepository.findBySharedItem_ShareIdAndUserId(shareId, userId);

        if (existing.isPresent()) {
            // Đã like → unlike
            likeRepository.delete(existing.get());
            return false;
        } else {
            // Chưa like → like
            SharedClothingItemLike like = SharedClothingItemLike.builder()
                    .sharedItem(record)
                    .userId(userId)
                    .build();
            likeRepository.save(like);
            return true;
        }
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────────

    private SharedClothingItemResponse mapToResponse(GroupSharedClothingItem record, UUID currentUserId) {
        boolean likedByMe = currentUserId != null &&
                likeRepository.findBySharedItem_ShareIdAndUserId(record.getShareId(), currentUserId).isPresent();
        long count = likeRepository.countBySharedItem_ShareId(record.getShareId());
        return buildResponse(record, count, likedByMe);
    }

    private SharedClothingItemResponse mapToResponseWithLikeSet(GroupSharedClothingItem record, Set<UUID> likedSet) {
        long count = likeRepository.countBySharedItem_ShareId(record.getShareId());
        boolean likedByMe = likedSet.contains(record.getShareId());
        return buildResponse(record, count, likedByMe);
    }

    private SharedClothingItemResponse buildResponse(GroupSharedClothingItem record, long likeCount, boolean likedByMe) {
        ClothingItem item = record.getClothingItem();
        return SharedClothingItemResponse.builder()
                .shareId(record.getShareId())
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .imageId(item.getImageId())
                .dominantColor(item.getDominantColor())
                .style(item.getStyle())
                .confidenceScore(item.getConfidenceScore())
                .groupId(record.getGroupId())
                .sharedByUserId(record.getSharedByUserId())
                .sharedAt(record.getSharedAt())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .build();
    }
}
