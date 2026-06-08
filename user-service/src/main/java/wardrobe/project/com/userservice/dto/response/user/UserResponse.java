package wardrobe.project.com.userservice.dto.response.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import wardrobe.project.com.userservice.enums.UserStatus;

import java.time.Instant;

@Getter
@Setter
@Builder
public class UserResponse {

    private String userId;
    private String email;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}