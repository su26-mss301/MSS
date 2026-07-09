package wardrobe.project.com.userservice.service.group;

import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupJoinRequest;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.request.group.InviteFriendGroupMemberRequest;
import wardrobe.project.com.userservice.dto.request.group.UpdateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupDetailResponse;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupInvitationResponse;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupJoinRequestResponse;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;

import java.util.List;

public interface FriendGroupService {
    FriendGroupResponse createGroup(CreateFriendGroupRequest request);
    List<FriendGroupResponse> getMyGroups();
    List<FriendGroupResponse> discoverGroups();

    void requestToJoinGroup(String groupId, CreateFriendGroupJoinRequest request);

    FriendGroupDetailResponse getGroupDetail(String groupId);

    void inviteMember(String groupId, InviteFriendGroupMemberRequest request);
    List<FriendGroupInvitationResponse> getMyPendingInvitations();

    void acceptInvitation(String invitationId);

    void declineInvitation(String invitationId);

    void cancelInvitation(String groupId, String invitationId);

    void removeMember(String groupId, String memberId);

    void deleteGroup(String groupId);
    List<FriendGroupJoinRequestResponse> getGroupJoinRequests(String groupId);
    void acceptJoinRequest(String requestId);

    void rejectJoinRequest(String requestId);

    void cancelMyJoinRequest(String requestId);
    void leaveGroup(String groupId);
    FriendGroupResponse updateGroup(String groupId, UpdateFriendGroupRequest request);

}
