package wardrobe.project.com.userservice.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Value("${server.servlet.keycloak.server-url}")
    private String serverUrl;

    @Value("${server.servlet.keycloak.admin-realm}")
    private String adminRealm;

    @Value("${server.servlet.keycloak.admin-client-id}")
    private String adminClientId;

    @Value("${server.servlet.keycloak.admin-client-secret}")
    private String adminClientSecret;

    @Bean
    public Keycloak keycloakAdmin() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }
}