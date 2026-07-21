package wardrobe.project.com.recommendationservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendItemRepository extends JpaRepository<RecommendItem, UUID> {
    List<RecommendItem> findByUserIdOrderByRecommendationScoreDesc(UUID userId);

    @Query("SELECT r FROM RecommendItem r WHERE r.userId = :userId AND r.event.id = :eventId ORDER BY r.recommendationScore DESC")
    List<RecommendItem> findRecommendationsForUserAndEvent(@Param("userId") UUID userId, @Param("eventId") UUID eventId);

    long countByGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT CAST(generated_at AS date) AS day, COUNT(*)
            FROM recommend_item
            WHERE generated_at >= :from
            GROUP BY CAST(generated_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countDailySince(@Param("from") LocalDateTime from);

    @Query(value = """
            SELECT TO_CHAR(generated_at, 'HH24') AS hour, COUNT(*)
            FROM recommend_item
            WHERE generated_at >= :from
            GROUP BY hour
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> countHourlySince(@Param("from") LocalDateTime from);

    @Query(value = """
            SELECT TO_CHAR(generated_at, 'YYYY-MM') AS month, COUNT(*)
            FROM recommend_item
            WHERE generated_at >= :from
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countMonthlySince(@Param("from") LocalDateTime from);

    @Query(value = """
            SELECT r FROM RecommendItem r
            JOIN FETCH r.outfit o
            LEFT JOIN FETCH r.event e
            WHERE (:type = 'all'
                OR (:type = 'personal' AND LOWER(o.outfitName) LIKE '%cá nhân%')
                OR (:type = 'group' AND LOWER(o.outfitName) LIKE '%nhóm%')
                OR (:type = 'event' AND LOWER(o.outfitName) NOT LIKE '%cá nhân%' AND LOWER(o.outfitName) NOT LIKE '%nhóm%'))
            """,
            countQuery = """
            SELECT COUNT(r) FROM RecommendItem r
            JOIN r.outfit o
            WHERE (:type = 'all'
                OR (:type = 'personal' AND LOWER(o.outfitName) LIKE '%cá nhân%')
                OR (:type = 'group' AND LOWER(o.outfitName) LIKE '%nhóm%')
                OR (:type = 'event' AND LOWER(o.outfitName) NOT LIKE '%cá nhân%' AND LOWER(o.outfitName) NOT LIKE '%nhóm%'))
            """)
    Page<RecommendItem> findHistory(@Param("type") String type, Pageable pageable);

    @Query("""
            SELECT COUNT(r) FROM RecommendItem r
            JOIN r.outfit o
            WHERE LOWER(o.outfitName) LIKE '%cá nhân%'
            """)
    long countPersonal();

    @Query("""
            SELECT COUNT(r) FROM RecommendItem r
            JOIN r.outfit o
            WHERE LOWER(o.outfitName) LIKE '%nhóm%'
            """)
    long countGroup();

    @Query("""
            SELECT COUNT(r) FROM RecommendItem r
            JOIN r.outfit o
            WHERE LOWER(o.outfitName) NOT LIKE '%cá nhân%'
              AND LOWER(o.outfitName) NOT LIKE '%nhóm%'
            """)
    long countEvent();
}
