package wardrobe.project.com.userservice.dto.response.group;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {

    private String memberId;
    private String userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String role;
    private Instant joinedAt;
}