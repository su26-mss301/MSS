package wardrobe.project.com.recommendationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.recommendationservice.entity.Outfit;

import java.util.UUID;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, UUID> {
}