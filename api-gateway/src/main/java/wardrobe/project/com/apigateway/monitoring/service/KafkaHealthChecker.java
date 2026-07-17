package wardrobe.project.com.apigateway.monitoring.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;
import wardrobe.project.com.apigateway.monitoring.dto.HealthStatus;
import wardrobe.project.com.apigateway.monitoring.dto.KafkaHealthResponse;


import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaHealthChecker {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthResponse check() {
        Instant startedAt = Instant.now();

        try (
                Admin admin = Admin.create(
                        kafkaAdmin.getConfigurationProperties()
                )
        ) {
            DescribeClusterResult clusterResult =
                    admin.describeCluster();

            int brokerCount = clusterResult
                    .nodes()
                    .get(3, TimeUnit.SECONDS)
                    .size();

            String clusterId = clusterResult
                    .clusterId()
                    .get(3, TimeUnit.SECONDS);

            Collection<ConsumerGroupListing> consumerGroups =
                    admin.listConsumerGroups()
                            .all()
                            .get(3, TimeUnit.SECONDS);

            long responseTimeMs = Duration.between(
                    startedAt,
                    Instant.now()
            ).toMillis();

            return KafkaHealthResponse.builder()
                    .status(HealthStatus.UP)
                    .brokerCount(brokerCount)
                    .consumerGroupCount(consumerGroups.size())
                    .clusterId(clusterId)
                    .responseTimeMs(responseTimeMs)
                    .message("Kafka broker đang hoạt động")
                    .build();

        } catch (Exception exception) {
            long responseTimeMs = Duration.between(
                    startedAt,
                    Instant.now()
            ).toMillis();

            return KafkaHealthResponse.builder()
                    .status(HealthStatus.DOWN)
                    .brokerCount(0)
                    .consumerGroupCount(0)
                    .clusterId(null)
                    .responseTimeMs(responseTimeMs)
                    .message("Không thể kết nối Kafka broker")
                    .build();
        }
    }
}