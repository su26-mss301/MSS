package wardrobe.project.com.wardrobeservice.service;

import wardrobe.project.com.wardrobeservice.dto.request.ShareClothingItemRequest;
import wardrobe.project.com.wardrobeservice.dto.response.SharedClothingItemResponse;

import java.util.List;
import java.util.UUID;

public interface GroupSharedClothingItemService {

    /**
     * Chia sẻ một ClothingItem vào group.
     * Nếu item đã được share vào group này (chưa unshare) thì trả về record hiện tại.
     */
    SharedClothingItemResponse shareItem(UUID userId, ShareClothingItemRequest request);

    /**
     * Lấy danh sách tất cả item đã được share vào group, mới nhất trước.
     * @param currentUserId dùng để tính likedByMe
     */
    List<SharedClothingItemResponse> getSharedItemsByGroup(String groupId, UUID currentUserId);

    /**
     * Unshare (soft-delete): chỉ người đã share mới được unshare.
     */
    void unshareItem(UUID shareId, UUID userId);

    /**
     * Toggle like: nếu chưa like → like; nếu đã like → unlike.
     * @return true nếu sau thao tác là liked, false nếu là unliked
     */
    boolean toggleLike(UUID shareId, UUID userId);
}

