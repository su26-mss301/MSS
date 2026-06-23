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
import wardrobe.project.com.storageservice.util.JwtUtil;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;
    private final JwtUtil jwtUtil;

    private String extractUserId(jakarta.servlet.http.HttpServletRequest request) {
        String xAuthUserId = request.getHeader("X-Auth-User-Id");
        if (xAuthUserId != null && !xAuthUserId.isBlank()) {
            return xAuthUserId;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            return jwtUtil.extractUserId(authHeader);
        }

        throw new IllegalArgumentException("Missing authentication context");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/storage/upload
    // Upload ảnh lên S3, lưu DB với status=DETECTING
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userId = extractUserId(request);
            log.info("Upload request from user={}, file={}", userId, file.getOriginalFilename());
            Image image = storageService.uploadImage(file, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(image));
        } catch (IllegalArgumentException e) {
            log.error("Unauthorized upload attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v1/storage/images/{id}/confirm
    // Xác nhận ảnh: DETECTING → DONE
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/images/{id}/confirm")
    public ResponseEntity<?> confirmImage(
            @PathVariable UUID id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userId = extractUserId(request);
            log.info("Confirm image id={} by user={}", id, userId);
            ImageResponse response = storageService.confirmImage(id, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(e.getMessage().contains("permission") ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error confirming image id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Confirm failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/storage/images
    // Lấy tất cả ảnh DONE của user hiện tại
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/images")
    public ResponseEntity<?> getAllImages(
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userId = extractUserId(request);
            log.info("List images for user={}", userId);
            List<Image> images = storageService.getImagesByUser(userId);
            List<ImageResponse> responses = images.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error listing images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to list images");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/storage/images/{id}
    // Xóa ảnh DONE của user hiện tại (không cho xóa DETECTING)
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable UUID id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userId = extractUserId(request);
            log.info("Delete image id={} by user={}", id, userId);
            storageService.deleteImage(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(e.getMessage().contains("permission") ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (IllegalStateException e) {
            // Ảnh đang DETECTING — không được xóa thủ công
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting image id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/storage/{id}
    // Lấy metadata ảnh (chỉ đúng owner)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getImageInfo(
            @PathVariable UUID id,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String userId = extractUserId(request);
            Image image = storageService.getImageInfo(id, userId);
            return ResponseEntity.ok(mapToResponse(image));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(e.getMessage().contains("permission") ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error retrieving image id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve image");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/storage/url/{id}
    // Lấy presigned URL (chỉ đúng owner)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/url/{id}")
    public ResponseEntity<?> getImageUrl(
            @PathVariable UUID id) {
        try {
            String url = storageService.getImageUrl(id);
            return ResponseEntity.ok(url);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(e.getMessage().contains("permission") ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error generating URL for image id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate URL");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal mapper
    // ─────────────────────────────────────────────────────────────────────────


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