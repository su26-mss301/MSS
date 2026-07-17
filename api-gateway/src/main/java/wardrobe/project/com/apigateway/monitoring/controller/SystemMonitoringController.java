package wardrobe.project.com.apigateway.monitoring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.monitoring.dto.SystemHealthResponse;
import wardrobe.project.com.apigateway.monitoring.service.SystemMonitoringService;

@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
public class SystemMonitoringController {

    private final SystemMonitoringService systemMonitoringService;

    @GetMapping("/health")
    public Mono<SystemHealthResponse> getSystemHealth() {
        return systemMonitoringService.getSystemHealth();
    }
}