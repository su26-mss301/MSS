package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.OutboxEvent;
import wardrobe.project.com.userservice.enums.OutboxStatus;

import java.util.List;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(
            OutboxStatus status
    );
}