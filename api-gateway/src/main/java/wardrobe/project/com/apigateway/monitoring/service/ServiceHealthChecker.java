package wardrobe.project.com.apigateway.monitoring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.monitoring.config.SystemMonitoringProperties;
import wardrobe.project.com.apigateway.monitoring.dto.HealthStatus;
import wardrobe.project.com.apigateway.monitoring.dto.ServiceHealthResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServiceHealthChecker {

    private final WebClient monitoringWebClient;

    public Mono<ServiceHealthResponse> check(
            SystemMonitoringProperties.ServiceConfig service
    ) {
        Instant startedAt = Instant.now();

        return monitoringWebClient
                .get()
                .uri(service.getUrl())
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    long responseTimeMs = Duration.between(
                            startedAt,
                            Instant.now()
                    ).toMillis();

                    Object rawStatus = body.get("status");

                    HealthStatus status = parseStatus(rawStatus);

                    return ServiceHealthResponse.builder()
                            .name(service.getName())
                            .serviceId(service.getServiceId())
                            .url(service.getUrl())
                            .status(status)
                            .responseTimeMs(responseTimeMs)
                            .message(
                                    status == HealthStatus.UP
                                            ? "Service đang hoạt động"
                                            : "Service báo trạng thái " + status
                            )
                            .build();
                })
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(exception ->
                        Mono.just(
                                ServiceHealthResponse.builder()
                                        .name(service.getName())
                                        .serviceId(service.getServiceId())
                                        .url(service.getUrl())
                                        .status(HealthStatus.DOWN)
                                        .responseTimeMs(null)
                                        .message("Không thể kết nối service")
                                        .build()
                        )
                );
    }

    private HealthStatus parseStatus(Object rawStatus) {
        if (rawStatus == null) {
            return HealthStatus.UNKNOWN;
        }

        try {
            return HealthStatus.valueOf(
                    rawStatus.toString().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            return HealthStatus.UNKNOWN;
        }
    }
}