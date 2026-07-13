package wardrobe.project.com.userservice.dto.response.admin;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {
    private String userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private String address;
    private String status;
    private String email;
    private String role;
    private String createdAt;
}
