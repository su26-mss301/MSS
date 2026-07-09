package wardrobe.project.com.userservice.dto.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyForgotPasswordOtpRequest {
    private String email;
    private String otp;
}