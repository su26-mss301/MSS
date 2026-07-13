package wardrobe.project.com.userservice.config.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.config.properties.DefaultAdminProperties;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.keycloak.KeycloakAdminProvisioningService;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final DefaultAdminProperties adminProperties;
    private final KeycloakAdminProvisioningService keycloakService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminProperties.isEnabled()) {
            log.info("Default admin initialization is disabled");
            return;
        }

        validateProperties();

        String keycloakUserId = createAdminWithRetry();

        User admin = userRepository
                .findByUserId(keycloakUserId)
                .orElseGet(() ->
                        userRepository
                                .findByEmail(adminProperties.getEmail())
                                .orElseGet(User::new)
                );

        admin.setUserId(keycloakUserId);
        admin.setEmail(adminProperties.getEmail());
        admin.setUsername(adminProperties.getUsername());
        admin.setFullName(adminProperties.getFullName());
        admin.setRole(Role.ROLE_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        User savedAdmin = userRepository.save(admin);

        log.info(
                "Default admin synchronized successfully. userId={}, keycloakId={}, email={}",
                savedAdmin.getUserId(),
                keycloakUserId,
                savedAdmin.getEmail()
        );
    }

    private void validateProperties() {
        if (isBlank(adminProperties.getEmail())
                || isBlank(adminProperties.getUsername())
                || isBlank(adminProperties.getPassword())) {
            throw new IllegalStateException(
                    "Default admin is enabled but email, username or password is missing"
            );
        }
    }

    private String createAdminWithRetry() {
        int maxAttempts = 10;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return keycloakService.createOrGetAdmin(
                        adminProperties.getEmail(),
                        adminProperties.getUsername(),
                        adminProperties.getPassword(),
                        adminProperties.getFullName()
                );
            } catch (Exception exception) {
                log.warn(
                        "Keycloak chưa sẵn sàng, lần thử {}/{}",
                        attempt,
                        maxAttempts
                );

                if (attempt == maxAttempts) {
                    throw exception;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Admin initialization interrupted",
                            interruptedException
                    );
                }
            }
        }

        throw new IllegalStateException("Could not initialize admin");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}