package wardrobe.project.com.recommendationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.entity.RecommendMemberOutfit;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendMemberOutfitRepository extends JpaRepository<RecommendMemberOutfit, UUID> {

    List<RecommendMemberOutfit> findByRecommendItemOrderByCreatedAtAsc(RecommendItem recommendItem);
}
