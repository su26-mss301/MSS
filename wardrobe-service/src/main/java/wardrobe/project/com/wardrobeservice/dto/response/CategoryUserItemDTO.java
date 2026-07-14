package wardrobe.project.com.wardrobeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUserItemDTO {
    private UUID userId;
    private long itemCount;
    /** Ngày thêm sớm nhất trong khoảng lọc */
    private String firstAddedAt;
    /** Ngày thêm gần nhất trong khoảng lọc */
    private String lastAddedAt;
}
