package wardrobe.project.com.storageservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.storageservice.model.Image;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {
}
