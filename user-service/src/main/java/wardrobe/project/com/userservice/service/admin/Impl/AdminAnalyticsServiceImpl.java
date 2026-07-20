package wardrobe.project.com.userservice.service.admin.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import wardrobe.project.com.userservice.dto.response.admin.AnalyticsSummaryResponse;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.admin.AdminAnalyticsService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;

    @Override
    public AnalyticsSummaryResponse getSummary() {
        Instant now = Instant.now();
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgo = now.minus(14, ChronoUnit.DAYS);

        long total = userRepository.count();
        long thisWeek = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(weekAgo, now);
        long lastWeek = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(twoWeeksAgo, weekAgo);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant dailyFrom = today.minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<String, Long> dailyCounts = toCountMap(
                userRepository.countDailySince(dailyFrom),
                0
        );

        LocalDate monthStart = today.withDayOfMonth(1);
        Instant monthlyFrom = monthStart.minusMonths(5).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<String, Long> monthlyCounts = toCountMap(
                userRepository.countMonthlySince(monthlyFrom),
                1
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
