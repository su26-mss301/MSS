package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.ClothingItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClothingItemRepository extends JpaRepository<ClothingItem, UUID> {
    List<ClothingItem> findByZone_ZoneId(UUID zoneId);
    List<ClothingItem> findByCategory_CategoryId(UUID categoryId);

    /** Lấy tất cả clothing items thuộc wardrobe của một user cụ thể */
    List<ClothingItem> findByZone_Wardrobe_UserId(UUID userId);

    @Modifying
    @Query(value = "UPDATE clothing_item SET deleted_at = NULL WHERE zone_id = :zoneId", nativeQuery = true)
    void restoreItemsByZoneId(UUID zoneId);

    @Modifying
    @Query(value = "UPDATE clothing_item SET deleted_at = NULL WHERE zone_id IN (SELECT zone_id FROM wardrobe_zone WHERE wardrobe_id = :wardrobeId)", nativeQuery = true)
    void restoreItemsByWardrobeId(UUID wardrobeId);
    
    @Modifying
    @Query(value = "DELETE FROM clothing_item WHERE deleted_at < CURRENT_DATE - INTERVAL '30 days'", nativeQuery = true)
    void purgeOldDeletedItems();

    /**
     * Đếm số clothing_item theo categoryId trong khoảng thời gian.
     * Trả về [categoryId (UUID), count (Long)].
     * @SQLRestriction("deleted_at IS NULL") áp dụng tự động.
     */
    @Query("SELECT ci.category.categoryId, COUNT(ci) " +
           "FROM ClothingItem ci " +
           "WHERE ci.category IS NOT NULL " +
           "AND ci.createdAt >= :from AND ci.createdAt < :to " +
           "GROUP BY ci.category.categoryId")
    List<Object[]> countByCategoryAndDateRange(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    /**
     * Đếm clothing_item theo user trong danh sách categoryId (gộp bản trùng tên).
     * Trả về [userId (UUID), count (Long), firstAddedAt (LocalDateTime), lastAddedAt (LocalDateTime)].
     */
    @Query("SELECT w.userId, COUNT(ci), MIN(ci.createdAt), MAX(ci.createdAt) " +
           "FROM ClothingItem ci " +
           "JOIN ci.zone z " +
           "JOIN z.wardrobe w " +
           "WHERE ci.category.categoryId IN :categoryIds " +
           "AND ci.createdAt >= :from AND ci.createdAt < :to " +
           "GROUP BY w.userId " +
           "ORDER BY MAX(ci.createdAt) DESC")
    List<Object[]> countByCategoryIdsGroupByUser(@Param("categoryIds") List<UUID> categoryIds,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT CAST(created_at AS date) AS day, COUNT(*)
            FROM clothing_item
            WHERE deleted_at IS NULL
              AND created_at >= :from
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countDailySince(@Param("from") LocalDateTime from);

    @Query(value = """
            SELECT TO_CHAR(created_at, 'YYYY-MM') AS month, COUNT(*)
            FROM clothing_item
            WHERE deleted_at IS NULL
              AND created_at >= :from
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countMonthlySince(@Param("from") LocalDateTime from);
}
