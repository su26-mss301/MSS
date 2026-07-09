package wardrobe.project.com.userservice.dto.response.group;

import lombok.Builder;
import lombok.Getter;
import wardrobe.project.com.userservice.enums.FriendGroupJoinRequestStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Builder
public class FriendGroupJoinRequestResponse {

    private String requestId;

    private String groupId;
    private String groupName;
    private String groupEmoji;

    private String requesterId;
    private String requesterName;
    private String requesterEmail;
    private String requesterAvatarUrl;

    private String message;
    private boolean previouslyKicked;

    private FriendGroupJoinRequestStatus status;

    private Instant expiredAt;
    private Instant createdAt;
}