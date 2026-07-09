package wardrobe.project.com.userservice.dto.response.group;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupActiveMemberResponse {

    private String userId;
    private String fullName;
    private String email;
    private String avatarUrl;

    private String mainStyle;
    private String mainStyleLabel;

    private String role;
}