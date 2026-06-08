package wardrobe.project.com.recommendationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendItemRepository extends JpaRepository<RecommendItem, UUID> {
    List<RecommendItem> findByUserIdOrderByRecommendationScoreDesc(UUID userId);

    @Query("SELECT r FROM RecommendItem r WHERE r.userId = :userId AND r.event.id = :eventId ORDER BY r.recommendationScore DESC")
    List<RecommendItem> findRecommendationsForUserAndEvent(@Param("userId") UUID userId, @Param("eventId") UUID eventId);
}