package wardrobe.project.com.apigateway.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wardrobe.project.com.apigateway.monitoring.dto.SystemHealthResponse;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardOverviewResponse {

    private LocalDateTime generatedAt;
    private KpiMetricResponse users;
    private KpiMetricResponse clothingItems;
    private KpiMetricResponse detections;
    private KpiMetricResponse recommendations;
    private List<DailyActivityPoint> dailyActivity;
    private List<MonthlyGrowthPoint> monthlyGrowth;
    private SystemHealthResponse systemHealth;
}
