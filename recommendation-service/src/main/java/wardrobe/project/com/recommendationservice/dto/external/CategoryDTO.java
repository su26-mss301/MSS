package wardrobe.project.com.recommendationservice.dto.external;
import lombok.Data;
import java.util.UUID;

@Data
public class CategoryDTO {
    private UUID categoryId;
    private String categoryName;
    private Integer priorityIndex;
}