package wardrobe.project.com.wardrobeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUsersResponseDTO {
    private String categoryName;
    private String granularity;
    private String from;
    private String to;
    private long totalUsers;
    private long totalItems;
    private List<CategoryUserItemDTO> users;
}
