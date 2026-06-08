package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.WardrobeZone;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardrobeZoneRepository extends JpaRepository<WardrobeZone, UUID> {
    List<WardrobeZone> findByWardrobe_WardrobeId(UUID wardrobeId);
    List<WardrobeZone> findByWardrobe_WardrobeIdAndZoneNameContainingIgnoreCase(UUID wardrobeId, String keyword);
    List<WardrobeZone> findByZoneNameContainingIgnoreCase(String keyword);
}
