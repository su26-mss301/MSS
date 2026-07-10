package wardrobe.project.com.userservice.dto.request.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFriendGroupRequest {

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(max = 100, message = "Tên nhóm không được vượt quá 100 ký tự")
    private String groupName;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    private String emoji;

    @NotEmpty(message = "Phong cách chủ đạo không được để trống")
    private List<String> primaryStyles;
}