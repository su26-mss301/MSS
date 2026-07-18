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
public class ServiceHealthResponse {

    private String name;

    private String serviceId;

    private String url;

    private HealthStatus status;

    private Long responseTimeMs;

    private String message;
}