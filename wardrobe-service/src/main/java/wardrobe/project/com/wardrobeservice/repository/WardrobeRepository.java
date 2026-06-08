package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.Wardrobe;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardrobeRepository extends JpaRepository<Wardrobe, UUID> {
    List<Wardrobe> findByUserId(UUID userId);
    List<Wardrobe> findByWardrobeNameContainingIgnoreCase(String keyword);
}
