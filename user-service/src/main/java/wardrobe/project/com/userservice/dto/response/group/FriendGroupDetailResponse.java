package wardrobe.project.com.userservice.dto.response.group;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendGroupDetailResponse {

    private String groupId;
    private String groupName;
    private String description;
    private String emoji;

    private String myRole;
    private Integer memberCount;

    private String primaryStyle;
    private String primaryStyleLabel;

    private String status;
    private Instant createdAt;

    private List<GroupStyleStatResponse> commonStyles;
    private List<String> colorPalette;
    private List<GroupActiveMemberResponse> activeMembers;
    private List<GroupMemberResponse> members;
}