package wardrobe.project.com.apigateway.monitoring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "system-monitoring")
public class SystemMonitoringProperties {

    private List<ServiceConfig> services = new ArrayList<>();

    @Getter
    @Setter
    public static class ServiceConfig {
        private String name;
        private String serviceId;
        private String url;
    }
}