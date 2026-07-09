package wardrobe.project.com.userservice.dto.response.group;

import lombok.*;
import wardrobe.project.com.userservice.enums.FriendGroupRole;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendGroupResponse {

    private String groupId;
    private String groupName;
    private String description;
    private String emoji;

    private String ownerId;
    private String ownerName;

    private long memberCount;
    private FriendGroupRole myRole;

    private Boolean active;
    private String createdAt;
}