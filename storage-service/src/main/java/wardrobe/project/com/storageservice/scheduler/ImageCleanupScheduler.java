package wardrobe.project.com.storageservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import wardrobe.project.com.storageservice.model.Image;
import wardrobe.project.com.storageservice.model.ImageStatus;
import wardrobe.project.com.storageservice.repository.ImageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageCleanupScheduler {

    private final ImageRepository imageRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${storage.cleanup.detecting-ttl-minutes:30}")
    private long detectingTtlMinutes;

    /**
     * Chạy định kỳ theo cấu hình scheduler-interval-ms (mặc định 5 phút).
     * Tìm và xóa tất cả ảnh có status=DETECTING quá thời gian TTL.
     */
    @Scheduled(fixedDelayString = "${storage.cleanup.scheduler-interval-ms:300000}")
    public void cleanupExpiredDetectingImages() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(detectingTtlMinutes);
        List<Image> expiredImages = imageRepository.findByStatusAndUploadedAtBefore(
                ImageStatus.DETECTING, cutoff);

        if (expiredImages.isEmpty()) {
            log.debug("Cleanup scheduler: no expired DETECTING images found.");
            return;
        }

        log.info("Cleanup scheduler: found {} expired DETECTING image(s) to remove.", expiredImages.size());

        int deleted = 0;
        int failed = 0;
        for (Image image : expiredImages) {
            try {
                // Xóa khỏi S3
                String objectKey = extractObjectKey(image.getImageUrl());
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build());

                // Xóa khỏi DB
                imageRepository.delete(image);
                deleted++;
                log.debug("Cleaned up expired image: id={}, user={}", image.getImageId(), image.getUserId());
            } catch (Exception e) {
                failed++;
                log.error("Failed to clean up image id={}: {}", image.getImageId(), e.getMessage());
            }
        }

        log.info("Cleanup scheduler complete: deleted={}, failed={}", deleted, failed);
    }

    private String extractObjectKey(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }
}
