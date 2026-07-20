package wardrobe.project.com.apigateway.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyGrowthPoint {

    private String month;
    private String monthLabel;
    private long users;
    private long items;
}
