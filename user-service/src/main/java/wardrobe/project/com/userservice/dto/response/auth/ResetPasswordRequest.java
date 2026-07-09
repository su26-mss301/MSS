package wardrobe.project.com.userservice.dto.response.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String resetToken;
    private String newPassword;
}