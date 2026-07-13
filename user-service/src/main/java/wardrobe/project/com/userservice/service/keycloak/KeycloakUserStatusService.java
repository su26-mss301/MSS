package wardrobe.project.com.userservice.service.keycloak;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeycloakUserStatusService {

    private final Keycloak keycloak;

    @Value("${server.servlet.keycloak.admin-realm}")
    private String realm;

    public void disableUser(String userId) {
        UserResource userResource = keycloak
                .realm(realm)
                .users()
                .get(userId);

        UserRepresentation representation =
                userResource.toRepresentation();

        representation.setEnabled(false);
        userResource.update(representation);

        // Xóa session hiện tại và làm refresh token không tiếp tục dùng được
        userResource.logout();
    }

    public void enableUser(String userId) {
        UserResource userResource = keycloak
                .realm(realm)
                .users()
                .get(userId);

        UserRepresentation representation =
                userResource.toRepresentation();

        representation.setEnabled(true);
        userResource.update(representation);
    }
}