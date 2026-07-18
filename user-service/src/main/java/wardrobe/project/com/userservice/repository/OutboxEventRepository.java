package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.OutboxEvent;
import wardrobe.project.com.userservice.enums.OutboxStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );

    @Modifying
    @Query("""
            DELETE FROM OutboxEvent event
            WHERE event.status = :status
              AND event.sentAt IS NOT NULL
              AND event.sentAt < :expiredBefore
            """)
    int deleteSentEventsBefore(
            @Param("status") OutboxStatus status,
            @Param("expiredBefore") LocalDateTime expiredBefore
    );
}