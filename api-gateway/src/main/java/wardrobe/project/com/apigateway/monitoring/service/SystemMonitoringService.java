package wardrobe.project.com.apigateway.monitoring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import wardrobe.project.com.apigateway.monitoring.config.SystemMonitoringProperties;
import wardrobe.project.com.apigateway.monitoring.dto.HealthStatus;
import wardrobe.project.com.apigateway.monitoring.dto.KafkaHealthResponse;
import wardrobe.project.com.apigateway.monitoring.dto.ServiceHealthResponse;
import wardrobe.project.com.apigateway.monitoring.dto.SystemHealthResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemMonitoringService {

    private final SystemMonitoringProperties properties;
    private final ServiceHealthChecker serviceHealthChecker;
    private final KafkaHealthChecker kafkaHealthChecker;

    public Mono<SystemHealthResponse> getSystemHealth() {
        Mono<List<ServiceHealthResponse>> servicesMono =
                Flux.fromIterable(properties.getServices())
                        .flatMap(serviceHealthChecker::check)
                        .collectList();

        Mono<KafkaHealthResponse> kafkaMono =
                Mono.fromCallable(kafkaHealthChecker::check)
                        .subscribeOn(Schedulers.boundedElastic());

        return Mono.zip(servicesMono, kafkaMono)
                .map(tuple -> {
                    List<ServiceHealthResponse> services = tuple.getT1();
                    KafkaHealthResponse kafka = tuple.getT2();

                    HealthStatus overallStatus =
                            calculateOverallStatus(services, kafka);

                    return SystemHealthResponse.builder()
                            .overallStatus(overallStatus)
                            .checkedAt(LocalDateTime.now())
                            .services(services)
                            .kafka(kafka)
                            .build();
                });
    }

    private HealthStatus calculateOverallStatus(
            List<ServiceHealthResponse> services,
            KafkaHealthResponse kafka
    ) {
        boolean allUp = services.stream()
                .allMatch(service ->
                        service.getStatus() == HealthStatus.UP
                );

        boolean allDown = services.stream()
                .allMatch(service ->
                        service.getStatus() == HealthStatus.DOWN
                );

        if (allUp && kafka.getStatus() == HealthStatus.UP) {
            return HealthStatus.UP;
        }

        if (allDown && kafka.getStatus() == HealthStatus.DOWN) {
            return HealthStatus.DOWN;
        }

        return HealthStatus.DEGRADED;
    }
}