package wardrobe.project.com.apigateway.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "admin-dashboard")
public class AdminDashboardProperties {

    private String userServiceUrl =
            "http://localhost:8081/api/v1/users/admin/analytics/summary";
    private String wardrobeServiceUrl =
            "http://localhost:8082/api/v1/wardrobe/admin/analytics/summary";
    private String recommendationServiceUrl =
            "http://localhost:8083/api/v1/recommendation/admin/analytics/summary";
    private String aiDetectionServiceUrl =
            "http://localhost:8084/admin/analytics/summary";
}
