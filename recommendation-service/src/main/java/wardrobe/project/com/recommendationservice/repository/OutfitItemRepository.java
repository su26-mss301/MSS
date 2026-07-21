package wardrobe.project.com.recommendationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.recommendationservice.entity.Outfit;
import wardrobe.project.com.recommendationservice.entity.OutfitItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutfitItemRepository extends JpaRepository<OutfitItem, UUID> {
    List<OutfitItem> findByOutfit(Outfit outfit);

    long countByOutfit(Outfit outfit);
}