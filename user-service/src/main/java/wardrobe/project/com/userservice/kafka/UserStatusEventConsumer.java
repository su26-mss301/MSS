package wardrobe.project.com.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.enums.UserStatus;
import wardrobe.project.com.userservice.event.UserStatusChangedEvent;
import wardrobe.project.com.userservice.service.keycloak.KeycloakUserStatusService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusEventConsumer {

    private final KeycloakUserStatusService keycloakUserStatusService;

    @KafkaListener(
            topics = KafkaTopics.USER_STATUS_CHANGED,
            groupId = "user-service-keycloak-sync"
    )
    public void consume(UserStatusChangedEvent event) {
        UserStatus status = UserStatus.valueOf(
                event.currentStatus()
        );

        switch (status) {
            case BLOCKED, INACTIVE ->
                    keycloakUserStatusService.disableUser(
                            event.userId()
                    );

            case ACTIVE ->
                    keycloakUserStatusService.enableUser(
                            event.userId()
                    );
        }

        log.info(
                "Synchronized user status with Keycloak: userId={}, status={}",
                event.userId(),
                event.currentStatus()
        );
    }
}