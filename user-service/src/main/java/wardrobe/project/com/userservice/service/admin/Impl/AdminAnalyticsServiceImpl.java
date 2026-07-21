package wardrobe.project.com.userservice.service.admin.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import wardrobe.project.com.userservice.dto.response.admin.AnalyticsSummaryResponse;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.admin.AdminAnalyticsService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;

    @Override
    public AnalyticsSummaryResponse getSummary(String granularity) {
        String period = normalizeGranularity(granularity);
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        PeriodRange range = resolvePeriod(period, now);

        long total = userRepository.count();
        long thisPeriod = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                range.currentStart(), range.currentEnd());
        long lastPeriod = userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                range.previousStart(), range.previousEnd());

        LocalDate today = now.toLocalDate();
        List<AnalyticsSummaryResponse.DailyCount> daily;
        if ("day".equals(period)) {
            Instant todayStart = today.atStartOfDay(ZONE).toInstant();
            Map<String, Long> hourlyCounts = toCountMap(
                    userRepository.countHourlySince(todayStart),
                    0
            );
            daily = buildHourlySeries(hourlyCounts);
        } else {
            Instant dailyFrom = buildDailyFrom(period, today);
            Map<String, Long> dailyCounts = toCountMap(
                    userRepository.countDailySince(dailyFrom),
                    0
            );
            daily = buildDailySeries(period, today, dailyCounts);
        }

        LocalDate monthStart = today.withDayOfMonth(1);        Instant monthlyFrom = monthStart.minusMonths(5).atStartOfDay(ZONE).toInstant();
        Map<String, Long> monthlyCounts = toCountMap(userRepository.countMonthlySince(monthlyFrom), 0);

        return AnalyticsSummaryResponse.builder()
                .total(total)
                .thisWeek(thisPeriod)
                .lastWeek(lastPeriod)
                .weekTrendPercent(calculateTrendPercent(thisPeriod, lastPeriod))
                .daily(daily)
                .monthly(buildMonthlySeries(monthStart, monthlyCounts))
                .build();
    }

    private String normalizeGranularity(String granularity) {
        if (granularity == null) {
            return "week";
        }
        return switch (granularity.toLowerCase()) {
            case "day", "month" -> granularity.toLowerCase();
            default -> "week";
        };
    }

    private PeriodRange resolvePeriod(String period, ZonedDateTime now) {
        return switch (period) {
            case "day" -> {
                ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(ZONE);
                ZonedDateTime yesterdayStart = todayStart.minusDays(1);
                yield new PeriodRange(
                        todayStart.toInstant(),
                        now.toInstant(),
                        yesterdayStart.toInstant(),
                        todayStart.toInstant()
                );
            }
            case "month" -> {
                ZonedDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay(ZONE);
                ZonedDateTime prevMonthStart = monthStart.minusMonths(1);
                yield new PeriodRange(
                        monthStart.toInstant(),
                        now.toInstant(),
                        prevMonthStart.toInstant(),
                        monthStart.toInstant()
                );
            }
            default -> {
                ZonedDateTime weekAgo = now.minusDays(7);
                ZonedDateTime twoWeeksAgo = now.minusDays(14);
                yield new PeriodRange(
                        weekAgo.toInstant(),
                        now.toInstant(),
                        twoWeeksAgo.toInstant(),
                        weekAgo.toInstant()
                );
            }
        };
    }

    private Instant buildDailyFrom(String period, LocalDate today) {
        if ("month".equals(period)) {
            return today.withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
        }
        return today.minusDays(6).atStartOfDay(ZONE).toInstant();
    }

    private Map<String, Long> toCountMap(List<Object[]> rows, int keyIndex) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put(String.valueOf(row[keyIndex]), ((Number) row[keyIndex + 1]).longValue());
        }
        return counts;
    }

    private List<AnalyticsSummaryResponse.DailyCount> buildHourlySeries(Map<String, Long> counts) {
        List<AnalyticsSummaryResponse.DailyCount> hourly = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            String key = String.format("%02d", hour);
            hourly.add(AnalyticsSummaryResponse.DailyCount.builder()
                    .date(key)
                    .count(counts.getOrDefault(key, 0L))
                    .build());
        }
        return hourly;
    }

    private List<AnalyticsSummaryResponse.DailyCount> buildDailySeries(
            String period,
            LocalDate today,
            Map<String, Long> counts
    ) {
        List<AnalyticsSummaryResponse.DailyCount> daily = new ArrayList<>();

        if ("month".equals(period)) {
            LocalDate cursor = today.withDayOfMonth(1);
            while (!cursor.isAfter(today)) {
                String key = cursor.toString();
                daily.add(AnalyticsSummaryResponse.DailyCount.builder()
                        .date(key)
                        .count(counts.getOrDefault(key, 0L))
                        .build());
                cursor = cursor.plusDays(1);
            }
            return daily;
        }

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

    private double calculateTrendPercent(long thisPeriod, long lastPeriod) {
        if (lastPeriod == 0) {
            return thisPeriod > 0 ? 100.0 : 0.0;
        }
        return Math.round(((thisPeriod - lastPeriod) * 1000.0 / lastPeriod)) / 10.0;
    }

    private record PeriodRange(
            Instant currentStart,
            Instant currentEnd,
            Instant previousStart,
            Instant previousEnd
    ) {
    }
}
