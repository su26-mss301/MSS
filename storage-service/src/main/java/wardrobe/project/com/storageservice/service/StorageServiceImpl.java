package wardrobe.project.com.storageservice.service;

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
import wardrobe.project.com.storageservice.model.Image;
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

    @Override
    @Transactional
    public Image uploadImage(MultipartFile file) throws Exception {
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
        
        // Generate unique object name
        String objectName = UUID.randomUUID().toString() + extension;

        // Upload to S3
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        }

        // Construct public URL
        String imageUrl;
        if (endpoint != null && !endpoint.isBlank()) {
            imageUrl = endpoint + "/" + bucketName + "/" + objectName;
        } else {
            imageUrl = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectName;
        }

        // Save metadata to database
        Image image = Image.builder()
                .fileName(originalFilename != null ? originalFilename : objectName)
                .imageUrl(imageUrl)
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();

        Image savedImage = imageRepository.save(image);
        log.info("Successfully uploaded image {} with ID {}", originalFilename, savedImage.getImageId());
        return savedImage;
    }

    @Override
    @Transactional(readOnly = true)
    public Image getImageInfo(UUID id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public String getImageUrl(UUID id) throws Exception {
        Image image = getImageInfo(id);
        return generatePresignedUrl(image.getImageUrl());
    }

    @Override
    public String generatePresignedUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return imageUrl;
        try {
            String objectName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            // Generate presigned URL for 7 days
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofDays(7))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectName)
                            .build())
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Error generating presigned URL for {}: {}", imageUrl, e.getMessage());
            return imageUrl;
        }
    }

    @Override
    @Transactional
    public void deleteImage(UUID id) throws Exception {
        Image image = getImageInfo(id);
        String imageUrl = image.getImageUrl();
        String objectName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

        // Delete from S3
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build());

        // Delete from database
        imageRepository.delete(image);
        log.info("Successfully deleted image with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Image> getAllImages() {
        return imageRepository.findAll();
    }
}
