package wardrobe.project.com.storageservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wardrobe.project.com.storageservice.dto.ImageResponse;
import wardrobe.project.com.storageservice.model.Image;
import wardrobe.project.com.storageservice.service.StorageService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    // POST /api/v1/storage/upload
    @PostMapping("/upload")
    public ResponseEntity<ImageResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("Received request to upload file: {}", file.getOriginalFilename());
        try {
            Image image = storageService.uploadImage(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(image));
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments for file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error occurred while uploading image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/v1/storage/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImageInfo(@PathVariable("id") UUID id) {
        log.info("Received request to get image metadata for ID: {}", id);
        try {
            Image image = storageService.getImageInfo(id);
            return ResponseEntity.ok(mapToResponse(image));
        } catch (IllegalArgumentException e) {
            log.error("Image not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error occurred while retrieving image metadata: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/v1/storage/url/{id}
    @GetMapping("/url/{id}")
    public ResponseEntity<String> getImageUrl(@PathVariable("id") UUID id) {
        log.info("Received request to generate URL for image ID: {}", id);
        try {
            String url = storageService.getImageUrl(id);
            return ResponseEntity.ok(url);
        } catch (IllegalArgumentException e) {
            log.error("Image not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error occurred while generating image URL: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DELETE /api/v1/storage/images/{id}  — aligned with FE: apiClient.delete(`/storage/images/${id}`)
    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable("id") UUID id) {
        log.info("Received request to delete image with ID: {}", id);
        try {
            storageService.deleteImage(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Image not found for deletion: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error occurred while deleting image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /api/v1/storage/images  — aligned with FE: apiClient.get<StoredImage[]>("/storage/images")
    @GetMapping("/images")
    public ResponseEntity<List<ImageResponse>> getAllImages() {
        log.info("Received request to list all images");
        try {
            List<Image> images = storageService.getAllImages();
            List<ImageResponse> responses = images.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error occurred while listing images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    // GET /api/v1/storage/files/{id} - Redirects to presigned URL
    @GetMapping("/files/{id}")
    public ResponseEntity<Void> redirectToFile(@PathVariable("id") UUID id) {
        log.info("Redirecting to image URL for ID: {}", id);
        try {
            String url = storageService.getImageUrl(id);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", url)
                    .build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ImageResponse mapToResponse(Image image) {
        return ImageResponse.builder()
                .id(image.getImageId())
                .name(image.getFileName())
                .url(storageService.generatePresignedUrl(image.getImageUrl()))
                .size(toMegabytes(image.getFileSize()))
                .createdAt(image.getUploadedAt())
                .build();
    }

    private Float toMegabytes(Long bytes) {
        if (bytes == null || bytes <= 0) return 0f;
        // Round to 1 decimal place, e.g. 2516582 → 2.4
        float mb = bytes / 1_048_576.0f;
        return Math.round(mb * 10) / 10f;
    }
}
