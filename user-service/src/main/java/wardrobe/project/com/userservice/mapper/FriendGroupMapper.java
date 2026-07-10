package wardrobe.project.com.userservice.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.enums.FriendGroupRole;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FriendGroupMapper {

    private final UserStylePreferenceMapper stylePreferenceMapper;

    public FriendGroupResponse toResponse(
            FriendGroup group,
            FriendGroupRole myRole,
            long memberCount
    ) {
        if (group == null) {
            return null;
        }

        List<String> styles = stylePreferenceMapper.fromJson(group.getPrimaryStyle());
        List<String> labels = styles.stream().map(this::toStyleLabel).toList();

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
                .primaryStyles(styles)
                .primaryStyleLabels(labels)
                .build();
    }

    private String toStyleLabel(String style) {
        if (style == null) return null;
        return switch (style) {
            case "MINIMAL"  -> "Tối Giản";
            case "CASUAL"   -> "Thường Ngày";
            case "OFFICE"   -> "Công Sở";
            case "ELEGANT"  -> "Trang Trọng";
            case "STREET"   -> "Đường Phố";
            case "BOHEMIAN" -> "Bohemian";
            case "SPORTY"   -> "Thể Thao";
            case "VINTAGE"  -> "Cổ Điển";
            default         -> style;
        };
    }
}