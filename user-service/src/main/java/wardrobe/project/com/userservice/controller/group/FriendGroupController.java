package wardrobe.project.com.userservice.controller.group;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupJoinRequest;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.request.group.InviteFriendGroupMemberRequest;
import wardrobe.project.com.userservice.dto.request.group.UpdateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupDetailResponse;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupInvitationResponse;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupJoinRequestResponse;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;
import wardrobe.project.com.userservice.service.group.FriendGroupService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friend-groups")
@RequiredArgsConstructor
public class FriendGroupController {

    private final FriendGroupService friendGroupService;

    @PostMapping
    public ApiResponse<FriendGroupResponse> createGroup(
            @Valid @RequestBody CreateFriendGroupRequest request
    ) {

        FriendGroupResponse response = friendGroupService.createGroup(request);

        return ApiResponse.success(response);
    }

    @GetMapping("/my")
    public ApiResponse<List<FriendGroupResponse>> getMyGroups(
    ) {
        return ApiResponse.success(friendGroupService.getMyGroups());
    }

    @GetMapping("/discover")
    public ApiResponse<List<FriendGroupResponse>> discoverGroups(
    ) {

        return ApiResponse.success(friendGroupService.discoverGroups());
    }



    @GetMapping("/{groupId}/detail")
    public ApiResponse<FriendGroupDetailResponse> getGroupDetail(
            @PathVariable String groupId
    ) {
        FriendGroupDetailResponse response = friendGroupService.getGroupDetail(groupId);

        return ApiResponse.success(response);
    }

    @PostMapping("/{groupId}/invitations")
    public ApiResponse<?> inviteMember(
            @PathVariable String groupId,
            @RequestBody InviteFriendGroupMemberRequest request
    ) {
        friendGroupService.inviteMember(groupId, request);
        return ApiResponse.success(Map.of("message", "Invitation sent successfully"));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ApiResponse<?> removeMember(
            @PathVariable String groupId,
            @PathVariable String memberId
    ) {
        friendGroupService.removeMember(groupId, memberId);
        return ApiResponse.success(Map.of("message", "Member removed successfully"));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<?> deleteGroup(@PathVariable String groupId) {
        friendGroupService.deleteGroup(groupId);
        return ApiResponse.success(Map.of("message", "Group deleted successfully"));
    }


    @GetMapping("/invitations/me")
    public ApiResponse<List<FriendGroupInvitationResponse>> getMyInvitations() {
        return ApiResponse.success(friendGroupService.getMyPendingInvitations());
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ApiResponse<?> acceptInvitation(@PathVariable String invitationId) {
        friendGroupService.acceptInvitation(invitationId);

        return ApiResponse.success(Map.of(
                "message", "Invitation accepted successfully"
        ));
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public ApiResponse<?> declineInvitation(@PathVariable String invitationId) {
        friendGroupService.declineInvitation(invitationId);

        return ApiResponse.success(Map.of(
                "message", "Invitation declined successfully"
        ));
    }

    @DeleteMapping("/{groupId}/invitations/{invitationId}")
    public ApiResponse<?> cancelInvitation(
            @PathVariable String groupId,
            @PathVariable String invitationId
    ) {
        friendGroupService.cancelInvitation(groupId, invitationId);

        return ApiResponse.success(Map.of(
                "message", "Invitation cancelled successfully"
        ));
    }

    @PostMapping("/{groupId}/join-requests")
    public ApiResponse<?> requestToJoinGroup(
            @PathVariable String groupId,
            @RequestBody CreateFriendGroupJoinRequest request
    ) {
        friendGroupService.requestToJoinGroup(groupId, request);

        return ApiResponse.success(Map.of(
                "message", "Yêu cầu tham gia đã được gửi"
        ));
    }

    @GetMapping("/{groupId}/join-requests")
    public ApiResponse<List<FriendGroupJoinRequestResponse>> getGroupJoinRequests(
            @PathVariable String groupId
    ) {
        return ApiResponse.success(friendGroupService.getGroupJoinRequests(groupId));
    }

    @PostMapping("/join-requests/{requestId}/accept")
    public ApiResponse<?> acceptJoinRequest(@PathVariable String requestId) {
        friendGroupService.acceptJoinRequest(requestId);

        return ApiResponse.success(Map.of(
                "message", "Đã chấp nhận yêu cầu tham gia"
        ));
    }

    @PostMapping("/join-requests/{requestId}/reject")
    public ApiResponse<?> rejectJoinRequest(@PathVariable String requestId) {
        friendGroupService.rejectJoinRequest(requestId);

        return ApiResponse.success(Map.of(
                "message", "Đã từ chối yêu cầu tham gia"
        ));
    }

    @PostMapping("/join-requests/{requestId}/cancel")
    public ApiResponse<?> cancelMyJoinRequest(@PathVariable String requestId) {
        friendGroupService.cancelMyJoinRequest(requestId);

        return ApiResponse.success(Map.of(
                "message", "Đã hủy yêu cầu tham gia"
        ));
    }

    @PostMapping("/{groupId}/leave")
    public ApiResponse<?> leaveGroup(@PathVariable String groupId) {
        friendGroupService.leaveGroup(groupId);

        return ApiResponse.success(Map.of(
                "message", "Bạn đã rời khỏi nhóm"
        ));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<FriendGroupResponse> updateGroup(
            @PathVariable String groupId,
            @RequestBody UpdateFriendGroupRequest request
    ) {
        return ApiResponse.success(friendGroupService.updateGroup(groupId, request));
    }
}