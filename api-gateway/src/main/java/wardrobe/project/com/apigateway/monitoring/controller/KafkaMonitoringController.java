package wardrobe.project.com.apigateway.monitoring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import wardrobe.project.com.apigateway.monitoring.dto.KafkaHealthResponse;
import wardrobe.project.com.apigateway.monitoring.service.KafkaHealthChecker;


@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
public class KafkaMonitoringController {

    private final KafkaHealthChecker kafkaHealthChecker;

    @GetMapping("/kafka")
    public Mono<KafkaHealthResponse> getKafkaHealth() {
        return Mono.fromCallable(kafkaHealthChecker::check)
                .subscribeOn(Schedulers.boundedElastic());
    }
}