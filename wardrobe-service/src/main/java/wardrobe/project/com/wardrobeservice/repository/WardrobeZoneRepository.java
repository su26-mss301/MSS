package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.WardrobeZone;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardrobeZoneRepository extends JpaRepository<WardrobeZone, UUID> {
    List<WardrobeZone> findByWardrobe_WardrobeId(UUID wardrobeId);
    List<WardrobeZone> findByWardrobe_WardrobeIdAndZoneNameContainingIgnoreCase(UUID wardrobeId, String keyword);
    List<WardrobeZone> findByZoneNameContainingIgnoreCase(String keyword);

    @Modifying
    @Query(value = "UPDATE wardrobe_zone SET deleted_at = NULL WHERE zone_id = :zoneId", nativeQuery = true)
    void restoreZone(UUID zoneId);

    @Modifying
    @Query(value = "UPDATE wardrobe_zone SET deleted_at = NULL WHERE wardrobe_id = :wardrobeId", nativeQuery = true)
    void restoreZonesByWardrobeId(UUID wardrobeId);
    
    @Modifying
    @Query(value = "DELETE FROM wardrobe_zone WHERE deleted_at < CURRENT_DATE - INTERVAL '30 days'", nativeQuery = true)
    void purgeOldDeletedZones();

    @Query(value = "SELECT wz.* FROM wardrobe_zone wz JOIN wardrobe w ON wz.wardrobe_id = w.wardrobe_id WHERE w.user_id = :userId AND wz.deleted_at IS NOT NULL", nativeQuery = true)
    List<WardrobeZone> findDeletedByUserId(UUID userId);

    @Query(value = "SELECT CASE WHEN w.deleted_at IS NOT NULL THEN 1 ELSE 0 END FROM wardrobe w JOIN wardrobe_zone wz ON w.wardrobe_id = wz.wardrobe_id WHERE wz.zone_id = :zoneId", nativeQuery = true)
    Integer isParentWardrobeDeleted(UUID zoneId);
}
