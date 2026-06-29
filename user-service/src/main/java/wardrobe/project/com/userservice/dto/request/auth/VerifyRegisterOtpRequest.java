package wardrobe.project.com.userservice.dto.request.auth;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRegisterOtpRequest {

    private String email;

    private String otp;
}