package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import wardrobe.project.com.wardrobeservice.dto.response.AnalyticsSummaryResponse;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.service.AdminAnalyticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ClothingItemRepository clothingItemRepository;

    @Override
    public AnalyticsSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime twoWeeksAgo = now.minusDays(14);

        long total = clothingItemRepository.count();
        long thisWeek = clothingItemRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(weekAgo, now);
        long lastWeek = clothingItemRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(twoWeeksAgo, weekAgo);

        LocalDate today = LocalDate.now();
        LocalDateTime dailyFrom = today.minusDays(6).atStartOfDay();
        Map<String, Long> dailyCounts = toCountMap(
                clothingItemRepository.countDailySince(dailyFrom),
                0
        );

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthlyFrom = monthStart.minusMonths(5).atStartOfDay();
        Map<String, Long> monthlyCounts = toCountMap(
                clothingItemRepository.countMonthlySince(monthlyFrom),
                0
        );

        return AnalyticsSummaryResponse.builder()
                .total(total)
                .thisWeek(thisWeek)
                .lastWeek(lastWeek)
                .weekTrendPercent(calculateTrendPercent(thisWeek, lastWeek))
                .daily(buildDailySeries(today, dailyCounts))
                .monthly(buildMonthlySeries(monthStart, monthlyCounts))
                .build();
    }

    private Map<String, Long> toCountMap(List<Object[]> rows, int keyIndex) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put(String.valueOf(row[keyIndex]), ((Number) row[keyIndex + 1]).longValue());
        }
        return counts;
    }

    private List<AnalyticsSummaryResponse.DailyCount> buildDailySeries(
            LocalDate today,
            Map<String, Long> counts
    ) {
        List<AnalyticsSummaryResponse.DailyCount> daily = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = date.toString();
            daily.add(AnalyticsSummaryResponse.DailyCount.builder()
                    .date(key)
                    .count(counts.getOrDefault(key, 0L))
                    .build());
        }
        return daily;
    }

    private List<AnalyticsSummaryResponse.MonthlyCount> buildMonthlySeries(
            LocalDate monthStart,
            Map<String, Long> counts
    ) {
        List<AnalyticsSummaryResponse.MonthlyCount> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = monthStart.minusMonths(i);
            String key = month.format(MONTH_FORMAT);
            monthly.add(AnalyticsSummaryResponse.MonthlyCount.builder()
                    .month(key)
                    .count(counts.getOrDefault(key, 0L))
                    .build());
        }
        return monthly;
    }

    private double calculateTrendPercent(long thisWeek, long lastWeek) {
        if (lastWeek == 0) {
            return thisWeek > 0 ? 100.0 : 0.0;
        }
        return Math.round(((thisWeek - lastWeek) * 1000.0 / lastWeek)) / 10.0;
    }
}
