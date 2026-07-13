package wardrobe.project.com.userservice.dto.request.admin;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserAdminRequest {
    private String username;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String status;
    private String role;
}
