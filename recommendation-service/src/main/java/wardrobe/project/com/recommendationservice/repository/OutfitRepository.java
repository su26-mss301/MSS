package wardrobe.project.com.recommendationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.recommendationservice.entity.Outfit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, UUID> {

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT CAST(created_at AS date) AS day, COUNT(*)
            FROM outfit
            WHERE created_at >= :from
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countDailySince(@Param("from") LocalDateTime from);

    @Query(value = """
            SELECT TO_CHAR(created_at, 'YYYY-MM') AS month, COUNT(*)
            FROM outfit
            WHERE created_at >= :from
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countMonthlySince(@Param("from") LocalDateTime from);
}