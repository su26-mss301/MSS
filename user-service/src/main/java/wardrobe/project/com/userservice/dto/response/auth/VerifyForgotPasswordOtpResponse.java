package wardrobe.project.com.userservice.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VerifyForgotPasswordOtpResponse {
    private String resetToken;
}