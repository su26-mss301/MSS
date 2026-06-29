package wardrobe.project.com.userservice.service.auth;

import wardrobe.project.com.userservice.dto.request.auth.LoginRequest;
import wardrobe.project.com.userservice.dto.request.auth.RegisterRequest;
import wardrobe.project.com.userservice.dto.request.auth.VerifyRegisterOtpRequest;
import wardrobe.project.com.userservice.dto.response.auth.KeycloakTokenResponse;

public interface AuthService {
    void register(RegisterRequest request);
    void verifyRegisterOtp(VerifyRegisterOtpRequest request);

    void resendCode(String email);

    KeycloakTokenResponse login(LoginRequest request);
    public void logout(String refreshToken);
    KeycloakTokenResponse refresh(String refreshToken);

}
