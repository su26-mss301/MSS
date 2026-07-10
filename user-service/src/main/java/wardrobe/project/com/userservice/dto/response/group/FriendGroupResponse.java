package wardrobe.project.com.userservice.dto.response.group;

import lombok.*;
import wardrobe.project.com.userservice.enums.FriendGroupRole;

import java.util.List;

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

    /** Phong cách chủ đạo (keys, e.g. ["MINIMAL", "OFFICE"]) */
    private List<String> primaryStyles;

    /** Nhãn tiếng Việt (e.g. ["Tối Giản", "Công Sở"]) */
    private List<String> primaryStyleLabels;
}