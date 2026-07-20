package wardrobe.project.com.apigateway.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {

    private long total;
    private long thisWeek;
    private long lastWeek;
    private double weekTrendPercent;
    private List<DailyCount> daily;
    private List<MonthlyCount> monthly;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private String date;
        private long count;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyCount {
        private String month;
        private long count;
    }

    public static AnalyticsSummaryResponse empty() {
        return AnalyticsSummaryResponse.builder()
                .total(0)
                .thisWeek(0)
                .lastWeek(0)
                .weekTrendPercent(0)
                .daily(List.of())
                .monthly(List.of())
                .build();
    }
}
