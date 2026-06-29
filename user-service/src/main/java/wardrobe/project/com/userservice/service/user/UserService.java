package wardrobe.project.com.userservice.service.user;

import wardrobe.project.com.userservice.dto.request.user.UpdateUserRequest;
import wardrobe.project.com.userservice.dto.response.auth.LoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;

public interface UserService {

    LoginResponse getCurrentUserByEmail(String email);
    UserResponse getMyInfo();

    UserResponse updateProfile(UpdateUserRequest request);

}
