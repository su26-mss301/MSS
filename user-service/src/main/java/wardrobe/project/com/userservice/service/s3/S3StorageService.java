package wardrobe.project.com.userservice.service.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {
    public String uploadAvatar(String userId, MultipartFile file);
}
