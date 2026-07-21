package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUserId(String userId);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant start, Instant end);

    @Query(value = """
            SELECT CAST(created_at AS date) AS day, COUNT(*)
            FROM users
            WHERE created_at >= :from
            GROUP BY CAST(created_at AS date)
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countDailySince(@Param("from") Instant from);

    @Query(value = """
            SELECT TO_CHAR(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24') AS hour, COUNT(*)
            FROM users
            WHERE created_at >= :from
            GROUP BY hour
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> countHourlySince(@Param("from") Instant from);

    @Query(value = """
            SELECT TO_CHAR(created_at AT TIME ZONE 'UTC', 'YYYY-MM') AS month, COUNT(*)
            FROM users
            WHERE created_at >= :from
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countMonthlySince(@Param("from") Instant from);
}
