package wardrobe.project.com.userservice.controller.auth;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import com.wardrobe.common.auth.Role;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserAuthTestController {

    @GetMapping("/auth-context-test")
    public Map<String, Object> authContextTest() {
        AuthContext authContext = AuthContextHolder.get();

        Map<String, Object> result = new HashMap<>();
        result.put("actorType", authContext.getActorType());
        result.put("userId", authContext.getUserId());
        result.put("email", authContext.getEmail());
        result.put("role", authContext.getRole());
        result.put("groups", authContext.getGroups());
        result.put("scopes", authContext.getScopes());
        result.put("requestId", authContext.getRequestId());

        return result;
    }

    @GetMapping("/admin-test")
    public Map<String, Object> adminTest() {
        AuthContext authContext = AuthContextHolder.get();

        authContext.requireRole(Role.ROLE_ADMIN);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "admin ok");
        result.put("userId", authContext.getUserId());

        return result;
    }

    @GetMapping("/public-test")
    public Map<String, Object> publicTest() {
        return Map.of("message", "user public ok");
    }
}