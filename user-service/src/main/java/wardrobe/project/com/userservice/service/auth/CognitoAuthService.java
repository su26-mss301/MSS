package wardrobe.project.com.userservice.service.auth;

import wardrobe.project.com.userservice.dto.request.auth.RegisterRequest;
import wardrobe.project.com.userservice.dto.response.auth.CognitoLoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;

public interface CognitoAuthService {
    CognitoLoginResponse login(String email, String password);

    UserResponse register(RegisterRequest request);

    void confirmRegister(String email, String otp);
    void resendCode(String email);

    String registerAndAddDefaultGroup(String email, String password);

    CognitoLoginResponse refresh(String refreshToken);
}
