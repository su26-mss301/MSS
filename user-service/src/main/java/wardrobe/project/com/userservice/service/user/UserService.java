package wardrobe.project.com.userservice.service.user;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import wardrobe.project.com.userservice.dto.request.user.UpdateUserRequest;
import wardrobe.project.com.userservice.dto.response.auth.LoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;

public interface UserService {

    LoginResponse getCurrentUserByEmail(String email);
    UserResponse getMyInfo();

    UserResponse updateProfile(UpdateUserRequest request);
    UserResponse uploadAvatar(MultipartFile file);

    UserResponse syncCurrentUser(Jwt jwt);

}
