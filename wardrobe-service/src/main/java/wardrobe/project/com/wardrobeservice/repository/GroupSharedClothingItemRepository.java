package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wardrobe.project.com.wardrobeservice.entity.GroupSharedClothingItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupSharedClothingItemRepository extends JpaRepository<GroupSharedClothingItem, UUID> {

    /**
     * Lấy tất cả items đã share vào một nhóm (chưa bị unshare), mới nhất trước
     */
    @Query("SELECT g FROM GroupSharedClothingItem g WHERE g.groupId = :groupId AND g.deletedAt IS NULL ORDER BY g.sharedAt DESC")
    List<GroupSharedClothingItem> findActiveByGroupId(@Param("groupId") String groupId);

    /**
     * Kiểm tra user đã share item này vào group chưa (tránh duplicate)
     */
    @Query("SELECT g FROM GroupSharedClothingItem g WHERE g.clothingItem.itemId = :itemId AND g.groupId = :groupId AND g.deletedAt IS NULL")
    Optional<GroupSharedClothingItem> findActiveByItemIdAndGroupId(@Param("itemId") UUID itemId, @Param("groupId") String groupId);
}
