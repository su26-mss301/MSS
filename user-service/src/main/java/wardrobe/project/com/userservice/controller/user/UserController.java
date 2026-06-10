package wardrobe.project.com.userservice.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.service.user.UserService;


@RestController
@RequestMapping()
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.syncCurrentUser();
    }

}