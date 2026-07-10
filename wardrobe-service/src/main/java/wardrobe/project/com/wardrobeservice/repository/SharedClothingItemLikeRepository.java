package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wardrobe.project.com.wardrobeservice.entity.SharedClothingItemLike;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SharedClothingItemLikeRepository extends JpaRepository<SharedClothingItemLike, UUID> {

    Optional<SharedClothingItemLike> findBySharedItem_ShareIdAndUserId(UUID shareId, UUID userId);

    long countBySharedItem_ShareId(UUID shareId);

    /**
     * Lấy tất cả shareId mà user đã like trong một danh sách
     */
    @Query("SELECT l.sharedItem.shareId FROM SharedClothingItemLike l WHERE l.sharedItem.shareId IN :shareIds AND l.userId = :userId")
    List<UUID> findLikedShareIdsByUserIdAndShareIdIn(@Param("shareIds") List<UUID> shareIds, @Param("userId") UUID userId);
}
