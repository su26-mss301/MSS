package wardrobe.project.com.userservice.service.group;

import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;

import java.util.List;

public interface FriendGroupService {
    FriendGroupResponse createGroup(CreateFriendGroupRequest request);
    List<FriendGroupResponse> getMyGroups();
    List<FriendGroupResponse> discoverGroups();

    FriendGroupResponse joinGroup( String groupId);
}
