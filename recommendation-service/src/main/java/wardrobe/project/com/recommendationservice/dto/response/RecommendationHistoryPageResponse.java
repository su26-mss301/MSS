package wardrobe.project.com.recommendationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationHistoryPageResponse {

    private List<RecommendationHistoryItemDTO> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    private long totalCount;
    private long personalCount;
    private long groupCount;
    private long eventCount;
}
