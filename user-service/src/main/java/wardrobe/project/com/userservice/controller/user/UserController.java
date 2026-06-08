package wardrobe.project.com.userservice.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.service.user.UserService;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        System.out.println("JWT CLAIMS = " + jwt.getClaims());
        System.out.println("JWT EMAIL = " + jwt.getClaimAsString("email"));
        System.out.println("JWT SUB = " + jwt.getSubject());

        return userService.syncCurrentUser(jwt);
    }

}