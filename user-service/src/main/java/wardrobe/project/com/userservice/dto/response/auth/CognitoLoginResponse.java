package wardrobe.project.com.userservice.dto.response.auth;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitoLoginResponse {
    String accessToken;
    String idToken;
    String refreshToken;
}
