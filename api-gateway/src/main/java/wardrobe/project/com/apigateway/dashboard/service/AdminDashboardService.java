package wardrobe.project.com.apigateway.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.dashboard.config.AdminDashboardProperties;
import wardrobe.project.com.apigateway.dashboard.dto.AdminDashboardOverviewResponse;
import wardrobe.project.com.apigateway.dashboard.dto.AnalyticsSummaryResponse;
import wardrobe.project.com.apigateway.dashboard.dto.DailyActivityPoint;
import wardrobe.project.com.apigateway.dashboard.dto.KpiMetricResponse;
import wardrobe.project.com.apigateway.dashboard.dto.MonthlyGrowthPoint;
import wardrobe.project.com.apigateway.dashboard.dto.ServiceApiResponse;
import wardrobe.project.com.apigateway.monitoring.dto.SystemHealthResponse;
import wardrobe.project.com.apigateway.monitoring.service.SystemMonitoringService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AdminDashboardProperties properties;
    private final WebClient monitoringWebClient;
    private final SystemMonitoringService systemMonitoringService;

    public Mono<AdminDashboardOverviewResponse> getOverview(
            HttpHeaders forwardHeaders,
            String granularity
    ) {
        String period = normalizeGranularity(granularity);
        Mono<AnalyticsSummaryResponse> usersMono =
                fetchWrappedSummary(properties.getUserServiceUrl(), forwardHeaders, period);
        Mono<AnalyticsSummaryResponse> wardrobeMono =
                fetchWrappedSummary(properties.getWardrobeServiceUrl(), forwardHeaders, period);
        Mono<AnalyticsSummaryResponse> recommendationMono =
                fetchWrappedSummary(properties.getRecommendationServiceUrl(), forwardHeaders, period);
        Mono<AnalyticsSummaryResponse> aiMono =
                fetchDirectSummary(properties.getAiDetectionServiceUrl(), forwardHeaders, period);
        Mono<SystemHealthResponse> healthMono = systemMonitoringService.getSystemHealth();

        return Mono.zip(usersMono, wardrobeMono, recommendationMono, aiMono, healthMono)
                .map(tuple -> buildOverview(
                        period,
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4(),
                        tuple.getT5()
                ));
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

    private Mono<AnalyticsSummaryResponse> fetchWrappedSummary(
            String url,
            HttpHeaders forwardHeaders,
            String granularity
    ) {
        String requestUrl = withGranularity(url, granularity);
        return monitoringWebClient.get()
                .uri(requestUrl)
                .headers(headers -> headers.addAll(forwardHeaders))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ServiceApiResponse<AnalyticsSummaryResponse>>() {})
                .map(response -> response.getData() != null
                        ? response.getData()
                        : AnalyticsSummaryResponse.empty())
                .onErrorResume(error -> {
                    log.warn("Failed to fetch analytics summary from {}: {}", requestUrl, error.getMessage());
                    return Mono.just(AnalyticsSummaryResponse.empty());
                });
    }

    private Mono<AnalyticsSummaryResponse> fetchDirectSummary(
            String url,
            HttpHeaders forwardHeaders,
            String granularity
    ) {
        String requestUrl = withGranularity(url, granularity);
        return monitoringWebClient.get()
                .uri(requestUrl)
                .headers(headers -> headers.addAll(forwardHeaders))
                .retrieve()
                .bodyToMono(AnalyticsSummaryResponse.class)
                .onErrorResume(error -> {
                    log.warn("Failed to fetch analytics summary from {}: {}", requestUrl, error.getMessage());
                    return Mono.just(AnalyticsSummaryResponse.empty());
                });
    }

    private String withGranularity(String url, String granularity) {
        return UriComponentsBuilder.fromUriString(url)
                .queryParam("granularity", granularity)
                .build()
                .toUriString();
    }

    private AdminDashboardOverviewResponse buildOverview(
            String granularity,
            AnalyticsSummaryResponse users,
            AnalyticsSummaryResponse wardrobe,
            AnalyticsSummaryResponse recommendations,
            AnalyticsSummaryResponse detections,
            SystemHealthResponse systemHealth
    ) {
        return AdminDashboardOverviewResponse.builder()
                .generatedAt(LocalDateTime.now())
                .granularity(granularity)
                .users(toKpi(users))
                .clothingItems(toKpi(wardrobe))
                .detections(toKpi(detections))
                .recommendations(toKpi(recommendations))
                .dailyActivity(buildDailyActivity(granularity, users, detections, recommendations))
                .monthlyGrowth(buildMonthlyGrowth(users, wardrobe))
                .systemHealth(systemHealth)
                .build();
    }

    private KpiMetricResponse toKpi(AnalyticsSummaryResponse summary) {
        return KpiMetricResponse.builder()
                .total(summary.getTotal())
                .thisWeek(summary.getThisWeek())
                .weekTrendPercent(summary.getWeekTrendPercent())
                .build();
    }

    private List<DailyActivityPoint> buildDailyActivity(
            String granularity,
            AnalyticsSummaryResponse users,
            AnalyticsSummaryResponse detections,
            AnalyticsSummaryResponse recommendations
    ) {
        Map<String, Long> userDaily = toDailyMap(users);
        Map<String, Long> detectionDaily = toDailyMap(detections);
        Map<String, Long> recommendationDaily = toDailyMap(recommendations);

        List<String> keys = new ArrayList<>();

        if ("day".equals(granularity)) {
            for (int hour = 0; hour < 24; hour++) {
                keys.add(String.format("%02d", hour));
            }
        } else {
            Set<String> dates = new LinkedHashSet<>();
            collectDates(dates, users);
            collectDates(dates, detections);
            collectDates(dates, recommendations);

            if (dates.isEmpty()) {
                LocalDate today = LocalDate.now();
                if ("month".equals(granularity)) {
                    LocalDate cursor = today.withDayOfMonth(1);
                    while (!cursor.isAfter(today)) {
                        dates.add(cursor.toString());
                        cursor = cursor.plusDays(1);
                    }
                } else {
                    for (int i = 6; i >= 0; i--) {
                        dates.add(today.minusDays(i).toString());
                    }
                }
            }
            keys.addAll(dates);
        }

        List<DailyActivityPoint> points = new ArrayList<>();
        for (String key : keys) {
            points.add(DailyActivityPoint.builder()
                    .date(key)
                    .dayLabel(formatActivityLabel(key, granularity))
                    .users(userDaily.getOrDefault(key, 0L))
                    .detections(detectionDaily.getOrDefault(key, 0L))
                    .recommendations(recommendationDaily.getOrDefault(key, 0L))
                    .build());
        }
        return points;
    }

    private String formatActivityLabel(String key, String granularity) {
        if ("day".equals(granularity)) {
            return Integer.parseInt(key) + "h";
        }
        if ("month".equals(granularity)) {
            return String.valueOf(LocalDate.parse(key).getDayOfMonth());
        }
        return formatDayLabel(key);
    }

    private void collectDates(Set<String> dates, AnalyticsSummaryResponse summary) {
        if (summary.getDaily() == null) {
            return;
        }
        summary.getDaily().forEach(point -> dates.add(point.getDate()));
    }

    private List<MonthlyGrowthPoint> buildMonthlyGrowth(
            AnalyticsSummaryResponse users,
            AnalyticsSummaryResponse wardrobe
    ) {
        Map<String, Long> userMonthly = toMonthlyMap(users);
        Map<String, Long> itemMonthly = toMonthlyMap(wardrobe);

        Set<String> months = new LinkedHashSet<>();
        if (users.getMonthly() != null) {
            users.getMonthly().forEach(point -> months.add(point.getMonth()));
        }
        if (wardrobe.getMonthly() != null) {
            wardrobe.getMonthly().forEach(point -> months.add(point.getMonth()));
        }

        if (months.isEmpty()) {
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            for (int i = 5; i >= 0; i--) {
                months.add(monthStart.minusMonths(i).format(MONTH_FORMAT));
            }
        }

        List<MonthlyGrowthPoint> points = new ArrayList<>();
        List<String> sortedMonths = new ArrayList<>(months);
        sortedMonths.sort(Comparator.naturalOrder());
        for (String month : sortedMonths) {
            points.add(MonthlyGrowthPoint.builder()
                    .month(month)
                    .monthLabel(formatMonthLabel(month))
                    .users(userMonthly.getOrDefault(month, 0L))
                    .items(itemMonthly.getOrDefault(month, 0L))
                    .build());
        }
        return points;
    }

    private Map<String, Long> toDailyMap(AnalyticsSummaryResponse summary) {
        Map<String, Long> map = new HashMap<>();
        if (summary.getDaily() == null) {
            return map;
        }
        summary.getDaily().forEach(point ->
                map.put(point.getDate(), point.getCount())
        );
        return map;
    }

    private Map<String, Long> toMonthlyMap(AnalyticsSummaryResponse summary) {
        Map<String, Long> map = new HashMap<>();
        if (summary.getMonthly() == null) {
            return map;
        }
        summary.getMonthly().forEach(point ->
                map.put(point.getMonth(), point.getCount())
        );
        return map;
    }

    private String formatDayLabel(String isoDate) {
        LocalDate date = LocalDate.parse(isoDate);
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private String formatMonthLabel(String monthKey) {
        int month = Integer.parseInt(monthKey.substring(5, 7));
        return "Thg " + month;
    }
}
