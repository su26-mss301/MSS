package wardrobe.project.com.userservice.dto.request.group;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFriendGroupRequest {
    private String groupName;
    private String description;
    private String emoji;
}