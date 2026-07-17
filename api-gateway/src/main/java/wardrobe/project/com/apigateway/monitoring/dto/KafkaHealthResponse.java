package wardrobe.project.com.apigateway.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaHealthResponse {

    private HealthStatus status;

    private Integer brokerCount;

    private Integer consumerGroupCount;

    private String clusterId;

    private Long responseTimeMs;

    private String message;
}