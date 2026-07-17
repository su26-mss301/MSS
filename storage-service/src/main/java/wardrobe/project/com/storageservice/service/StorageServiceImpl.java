package wardrobe.project.com.storageservice.service;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import wardrobe.project.com.storageservice.dto.ImageResponse;
import wardrobe.project.com.storageservice.model.Image;
import wardrobe.project.com.storageservice.model.ImageStatus;
import wardrobe.project.com.storageservice.repository.ImageRepository;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ImageRepository imageRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    // ─────────────────────────────────────────────────────────────────────────
    // Upload
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Image uploadImage(MultipartFile file, String userId) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // Ensure bucket exists
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Created S3 bucket: {}", bucketName);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectKey = UUID.randomUUID() + extension;

        // Upload to S3
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        }

        // Build public URL (stored in DB — presigned URL generated on read)
        String imageUrl;
        if (endpoint != null && !endpoint.isBlank()) {
            imageUrl = endpoint + "/" + bucketName + "/" + objectKey;
        } else {
            imageUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectKey;
        }

        // Save metadata with status=DETECTING
        Image image = Image.builder()
                .fileName(originalFilename != null ? originalFilename : objectKey)
                .imageUrl(imageUrl)
                .fileSize(file.getSize())
                .userId(userId)
                .status(ImageStatus.DETECTING)
                .uploadedAt(LocalDateTime.now())
                .build();

        Image saved = imageRepository.save(image);
        log.info("Uploaded image id={} for user={} with status=DETECTING", saved.getImageId(), userId);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Confirm
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ImageResponse confirmImage(UUID id, String userId) {
        Image image = findOwnedImage(id, userId);
        if (image.getStatus() == ImageStatus.DONE) {
            log.warn("Image id={} is already DONE", id);
            return mapToResponse(image);
        }
        image.setStatus(ImageStatus.DONE);
        imageRepository.save(image);
        log.info("Confirmed image id={} for user={}", id, userId);
        return mapToResponse(image);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Image getImageInfo(UUID id, String userId) {
        return findReadableImage(id, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getImageUrl(UUID id, String userId) throws Exception {
        Image image = findReadableImage(id, userId);
        return generatePresignedUrl(image.getImageUrl());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Image> getImagesByUser(String userId) {
        return imageRepository.findByUserIdAndStatus(userId, ImageStatus.DONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteImage(UUID id, String userId) throws Exception {
        Image image = findOwnedImage(id, userId);

        if (image.getStatus() == ImageStatus.DETECTING) {
            throw new IllegalStateException("Cannot manually delete an image in DETECTING status. It will be cleaned up automatically.");
        }

        String objectKey = image.getImageUrl().substring(image.getImageUrl().lastIndexOf("/") + 1);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());

        imageRepository.delete(image);
        log.info("Deleted image id={} by user={}", id, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Presigned URL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String generatePresignedUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return imageUrl;
        try {
            String objectKey = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofDays(7))
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(objectKey)
                                    .build())
                            .build()
            );
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Error generating presigned URL for {}: {}", imageUrl, e.getMessage());
            return imageUrl;
        }
    }

    @Override
    @Transactional
    public void confirmImageFromEvent(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        Image image = imageRepository
                .findById(UUID.fromString(imageId))
                .orElseThrow(() ->
                        new RuntimeException("Image not found: " + imageId)
                );

        if (image.getStatus() == ImageStatus.DONE) {
            return;
        }

        image.setStatus(ImageStatus.DONE);
        imageRepository.save(image);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Tìm ảnh và xác minh quyền sở hữu, ném exception nếu không thấy hoặc không phải chủ */
    private Image findOwnedImage(UUID id, String userId) {
        return imageRepository.findByImageIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Image not found or you do not have permission to access it"));
    }

    /** Tìm ảnh, cho phép đọc nếu là owner HOẶC ảnh đã DONE (cho phép share) */
    private Image findReadableImage(UUID id, String userId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        
        if (image.getStatus() == ImageStatus.DETECTING && !userId.equals(image.getUserId())) {
            throw new IllegalArgumentException("You do not have permission to access this temporary image");
        }
        
        return image;
    }

    private ImageResponse mapToResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getImageId())
                .name(image.getFileName())
                .url(generatePresignedUrl(image.getImageUrl()))
                .size(toMegabytes(image.getFileSize()))
                .status(image.getStatus())
                .createdAt(image.getUploadedAt())
                .build();
    }

    private Float toMegabytes(Long bytes) {
        if (bytes == null || bytes <= 0) return 0f;
        float mb = bytes / 1_048_576.0f;
        return Math.round(mb * 10) / 10f;
    }
}