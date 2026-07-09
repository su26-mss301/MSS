package wardrobe.project.com.userservice.dto.response.auth;

import lombok.*;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String userId;

    private String email;

    private String username;

    private String fullName;

    private String avatarUrl;

    private String phoneNumber;

    private UserStatus status;

    private Role role;
}
