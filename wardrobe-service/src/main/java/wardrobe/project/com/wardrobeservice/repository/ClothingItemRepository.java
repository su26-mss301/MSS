package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.ClothingItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClothingItemRepository extends JpaRepository<ClothingItem, UUID> {
    List<ClothingItem> findByZone_ZoneId(UUID zoneId);
    List<ClothingItem> findByCategory_CategoryId(UUID categoryId);

    @Modifying
    @Query(value = "UPDATE clothing_item SET deleted_at = NULL WHERE zone_id = :zoneId", nativeQuery = true)
    void restoreItemsByZoneId(UUID zoneId);

    @Modifying
    @Query(value = "UPDATE clothing_item SET deleted_at = NULL WHERE zone_id IN (SELECT zone_id FROM wardrobe_zone WHERE wardrobe_id = :wardrobeId)", nativeQuery = true)
    void restoreItemsByWardrobeId(UUID wardrobeId);
    
    @Modifying
    @Query(value = "DELETE FROM clothing_item WHERE deleted_at < CURRENT_DATE - INTERVAL '30 days'", nativeQuery = true)
    void purgeOldDeletedItems();
}
