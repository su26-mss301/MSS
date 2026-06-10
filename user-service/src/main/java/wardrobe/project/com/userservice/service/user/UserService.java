package wardrobe.project.com.userservice.service.user;

import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import org.springframework.security.oauth2.jwt.Jwt;
public interface UserService {
    UserResponse syncCurrentUser();
}
