package wardrobe.project.com.userservice.dto.response.group;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupStyleStatResponse {

    private String styleName;
    private String label;
    private Integer percentage;
}