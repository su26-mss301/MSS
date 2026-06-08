package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.ClothingItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClothingItemRepository extends JpaRepository<ClothingItem, UUID> {
    List<ClothingItem> findByZone_ZoneId(UUID zoneId);
    List<ClothingItem> findByCategory_CategoryId(UUID categoryId);
}
