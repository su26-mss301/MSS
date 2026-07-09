package wardrobe.project.com.userservice.dto.response.group;

import lombok.Builder;
import lombok.Getter;
import wardrobe.project.com.userservice.enums.FriendGroupInvitationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class FriendGroupInvitationResponse {

    private String invitationId;

    private String groupId;
    private String groupName;
    private String groupEmoji;

    private String inviterId;
    private String inviterName;
    private String inviterEmail;

    private String inviteeId;
    private String inviteeName;
    private String inviteeEmail;

    private FriendGroupInvitationStatus status;

    private Instant expiredAt;
    private Instant createdAt;
}