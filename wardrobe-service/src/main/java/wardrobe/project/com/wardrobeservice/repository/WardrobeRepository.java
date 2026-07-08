package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.Wardrobe;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardrobeRepository extends JpaRepository<Wardrobe, UUID> {
    List<Wardrobe> findByUserId(UUID userId);
    List<Wardrobe> findByWardrobeNameContainingIgnoreCase(String keyword);
    List<Wardrobe> findByUserIdAndWardrobeNameContainingIgnoreCase(UUID userId, String keyword);

    @Modifying
    @Query(value = "UPDATE wardrobe SET deleted_at = NULL WHERE wardrobe_id = :wardrobeId", nativeQuery = true)
    void restoreWardrobe(UUID wardrobeId);
    
    @Modifying
    @Query(value = "DELETE FROM wardrobe WHERE deleted_at < CURRENT_DATE - INTERVAL '30 days'", nativeQuery = true)
    void purgeOldDeletedWardrobes();

    @Query(value = "SELECT * FROM wardrobe WHERE user_id = :userId AND deleted_at IS NOT NULL", nativeQuery = true)
    List<Wardrobe> findDeletedByUserId(UUID userId);
}
