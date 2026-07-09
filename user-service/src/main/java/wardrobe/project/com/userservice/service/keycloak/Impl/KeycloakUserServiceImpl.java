package wardrobe.project.com.userservice.service.keycloak.Impl;



import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wardrobe.project.com.userservice.dto.request.user.CreateUserRequest;
import wardrobe.project.com.userservice.service.keycloak.KeycloakUserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakUserServiceImpl implements KeycloakUserService {

    private final Keycloak keycloakAdmin;

    @Value("${server.servlet.keycloak.app-realm}")
    private String appRealm;

    public boolean existsByEmail(String email) {
        List<UserRepresentation> users = keycloakAdmin.realm(appRealm)
                .users()
                .searchByEmail(email, true);

        return !users.isEmpty();
    }

    public String createUser(CreateUserRequest request) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setEnabled(true);
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        user.setCredentials(List.of(credential));

        Response response = keycloakAdmin.realm(appRealm)
                .users()
                .create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Create Keycloak user failed. Status = " + response.getStatus());
        }

        String keycloakUserId = CreatedResponseUtil.getCreatedId(response);

        assignRealmRole(keycloakUserId);

        return keycloakUserId;
    }

    @Override
    public void resetPasswordByEmail(String email, String newPassword) {
        List<UserRepresentation> users = keycloakAdmin.realm(appRealm)
                .users()
                .searchByEmail(email, true);

        if (users.isEmpty()) {
            throw new RuntimeException("User not found in Keycloak");
        }

        String keycloakUserId = users.getFirst().getId();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        keycloakAdmin.realm(appRealm)
                .users()
                .get(keycloakUserId)
                .resetPassword(credential);
    }

    private void assignRealmRole(String userId) {
        try {
            RoleRepresentation role = keycloakAdmin.realm(appRealm)
                    .roles()
                    .get("ROLE_USER")
                    .toRepresentation();

            keycloakAdmin.realm(appRealm)
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));
        } catch (Exception e) {
            throw new RuntimeException("Assign role failed: " + "ROLE_USER" + ", userId = " + userId, e);
        }
    }
}