package wardrobe.project.com.userservice.service.auth;

import wardrobe.project.com.userservice.dto.response.auth.CognitoLoginResponse;

public interface CognitoAuthService {
    CognitoLoginResponse login(String email, String password);
    void register(String email, String password);

    void confirmRegister(String email, String otp);
    void resendCode(String email);
}
