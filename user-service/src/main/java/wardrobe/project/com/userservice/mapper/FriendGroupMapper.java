package wardrobe.project.com.userservice.mapper;

import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.enums.FriendGroupRole;

@Component
public class FriendGroupMapper {

    public FriendGroupResponse toResponse(
            FriendGroup group,
            FriendGroupRole myRole,
            long memberCount
    ) {
        if (group == null) {
            return null;
        }

        return FriendGroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .emoji(group.getEmoji())
                .ownerId(group.getOwner().getUserId())
                .ownerName(group.getOwner().getFullName())
                .memberCount(memberCount)
                .myRole(myRole)
                .active(group.getActive())
                .createdAt(
                        group.getCreatedAt() == null
                                ? null
                                : group.getCreatedAt().toString()
                )
                .build();
    }
}