package wardrobe.project.com.userservice.dto.request.auth;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendCodeRequest {
    private String email;
}
