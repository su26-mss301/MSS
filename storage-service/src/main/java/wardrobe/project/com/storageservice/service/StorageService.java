package wardrobe.project.com.storageservice.service;

import org.springframework.web.multipart.MultipartFile;
import wardrobe.project.com.storageservice.dto.ImageResponse;
import wardrobe.project.com.storageservice.model.Image;

import java.util.List;
import java.util.UUID;

public interface StorageService {

    /** Upload ảnh lên S3, lưu DB với status=DETECTING */
    Image uploadImage(MultipartFile file, String userId) throws Exception;

    /** Xác nhận ảnh: DETECTING → DONE (chỉ đúng owner) */
    ImageResponse confirmImage(UUID id, String userId);

    /** Lấy metadata ảnh (chỉ đúng owner) */
    Image getImageInfo(UUID id, String userId);

    /** Lấy presigned URL của ảnh (cho phép share nếu DONE) */
    String getImageUrl(UUID id, String userId) throws Exception;

    /** Xóa ảnh DONE khỏi S3 + DB (chỉ đúng owner, không xóa DETECTING) */
    void deleteImage(UUID id, String userId) throws Exception;

    /** Lấy tất cả ảnh DONE của 1 user */
    List<Image> getImagesByUser(String userId);

    /** Tạo presigned URL từ S3 URL gốc (dùng nội bộ) */
    String generatePresignedUrl(String imageUrl);

    void confirmImageFromEvent(String imageId);
}
