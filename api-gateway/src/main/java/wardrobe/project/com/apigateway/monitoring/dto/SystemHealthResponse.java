package wardrobe.project.com.apigateway.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {

    private HealthStatus overallStatus;

    private LocalDateTime checkedAt;

    private List<ServiceHealthResponse> services;

    private KafkaHealthResponse kafka;
}