package wardrobe.project.com.storageservice.service;

import org.springframework.web.multipart.MultipartFile;
import wardrobe.project.com.storageservice.model.Image;
import java.util.List;
import java.util.UUID;

public interface StorageService {
    Image uploadImage(MultipartFile file) throws Exception;
    Image getImageInfo(UUID id);
    String getImageUrl(UUID id) throws Exception;
    void deleteImage(UUID id) throws Exception;
    List<Image> getAllImages();
    String generatePresignedUrl(String imageUrl);
}
