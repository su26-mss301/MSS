package wardrobe.project.com.storageservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.storageservice.model.Image;
import wardrobe.project.com.storageservice.model.ImageStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

    // Lấy tất cả ảnh của 1 user theo status
    List<Image> findByUserIdAndStatus(String userId, ImageStatus status);

    // Scheduler dùng: tìm ảnh DETECTING quá hạn để dọn dẹp
    List<Image> findByStatusAndUploadedAtBefore(ImageStatus status, LocalDateTime cutoff);

    // Tìm ảnh theo id và userId (kiểm tra quyền sở hữu)
    Optional<Image> findByImageIdAndUserId(UUID imageId, String userId);
}
