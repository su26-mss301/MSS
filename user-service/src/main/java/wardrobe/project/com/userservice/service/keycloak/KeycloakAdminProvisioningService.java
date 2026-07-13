package wardrobe.project.com.userservice.service.keycloak;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakAdminProvisioningService {

    private final Keycloak keycloak;

    @Value("${server.servlet.keycloak.admin-realm}")
    private String realmName;

    public String createOrGetAdmin(
            String email,
            String username,
            String password,
            String fullName
    ) {
        RealmResource realm = keycloak.realm(realmName);

        List<UserRepresentation> existingUsers =
                realm.users().searchByEmail(email, true);

        UserRepresentation user;

        if (!existingUsers.isEmpty()) {
            user = existingUsers.getFirst();
        } else {
            user = createAdminUser(
                    realm,
                    email,
                    username,
                    password,
                    fullName
            );
        }

        assignAdminRole(realm, user.getId());

        return user.getId();
    }

    private UserRepresentation createAdminUser(
            RealmResource realm,
            String email,
            String username,
            String password,
            String fullName
    ) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);
        user.setEmailVerified(true);

        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);

            user.setFirstName(parts[0]);

            if (parts.length > 1) {
                user.setLastName(parts[1]);
            }
        }

        CredentialRepresentation credential =
                new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        user.setCredentials(List.of(credential));

        try (Response response = realm.users().create(user)) {
            if (response.getStatus() != 201) {
                throw new IllegalStateException(
                        "Không thể tạo admin trên Keycloak. HTTP status: "
                                + response.getStatus()
                );
            }

            String location = response.getLocation().getPath();

            String userId = location.substring(
                    location.lastIndexOf('/') + 1
            );

            return realm.users()
                    .get(userId)
                    .toRepresentation();
        }
    }

    private void assignAdminRole(
            RealmResource realm,
            String keycloakUserId
    ) {
        RoleRepresentation adminRole;

        try {
            adminRole = realm.roles()
                    .get("ADMIN")
                    .toRepresentation();
        } catch (Exception exception) {
            adminRole = new RoleRepresentation();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Application administrator");

            realm.roles().create(adminRole);

            adminRole = realm.roles()
                    .get("ADMIN")
                    .toRepresentation();
        }

        boolean alreadyAssigned = realm.users()
                .get(keycloakUserId)
                .roles()
                .realmLevel()
                .listAll()
                .stream()
                .anyMatch(role ->
                        role.getName().equalsIgnoreCase("ADMIN")
                );

        if (!alreadyAssigned) {
            realm.users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(adminRole));
        }
    }
}