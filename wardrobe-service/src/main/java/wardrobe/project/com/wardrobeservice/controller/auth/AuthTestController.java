package wardrobe.project.com.wardrobeservice.controller.auth;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import com.wardrobe.common.auth.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthTestController {

    @GetMapping("/auth-context-test")
    public Map<String, Object> testAuthContext() {
        AuthContext authContext = AuthContextHolder.get();

        return Map.of(
                "actorType", authContext.getActorType(),
                "userId", authContext.getUserId(),
                "role", authContext.getRole(),
                "groups", authContext.getGroups(),
                "scopes", authContext.getScopes(),
                "requestId", authContext.getRequestId()
        );
    }

    @GetMapping("/public-test")
    public Map<String, Object> publicTest() {
        return Map.of("message", "public ok");
    }

    @GetMapping("/admin-test")
    public Map<String, Object> adminTest() {
        AuthContext authContext = AuthContextHolder.get();

        authContext.requireRole(Role.ROLE_ADMIN);

        return Map.of(
                "message", "admin ok",
                "userId", authContext.getUserId()
        );
    }
}