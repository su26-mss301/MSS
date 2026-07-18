package wardrobe.project.com.wardrobeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.wardrobeservice.entity.ProcessedKafkaEvent;

@Repository
public interface ProcessedKafkaEventRepository
        extends JpaRepository<ProcessedKafkaEvent, Long> {

    boolean existsByEventId(String eventId);
}