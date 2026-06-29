package wardrobe.project.com.userservice.dto.request.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String email;
    private String username;
    private String password;
    private String firstName;
}
