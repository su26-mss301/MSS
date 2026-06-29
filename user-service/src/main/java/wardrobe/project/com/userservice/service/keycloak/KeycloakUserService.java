package wardrobe.project.com.userservice.service.keycloak;

import wardrobe.project.com.userservice.dto.request.user.CreateUserRequest;

public interface KeycloakUserService {
    boolean existsByEmail(String email);

    String createUser(CreateUserRequest request);
}
