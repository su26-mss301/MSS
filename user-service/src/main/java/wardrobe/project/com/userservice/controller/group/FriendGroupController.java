package wardrobe.project.com.userservice.controller.group;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;
import wardrobe.project.com.userservice.service.group.FriendGroupService;

import java.util.List;

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

    @PostMapping("/{groupId}/join")
    public ApiResponse<FriendGroupResponse> joinGroup(
            @PathVariable String groupId
    ) {
        FriendGroupResponse response = friendGroupService.joinGroup(groupId);

        return  ApiResponse.success(response);
    }
}