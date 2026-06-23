package wardrobe.project.com.userservice.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.request.auth.ConfirmRegisterRequest;
import wardrobe.project.com.userservice.dto.request.auth.LoginRequest;
import wardrobe.project.com.userservice.dto.request.auth.RegisterRequest;
import wardrobe.project.com.userservice.dto.request.auth.ResendCodeRequest;
import wardrobe.project.com.userservice.dto.response.auth.LoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.service.auth.CognitoAuthService;
import wardrobe.project.com.userservice.service.user.UserService;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CognitoAuthService cognitoAuthService;
    private final UserService userService;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String sameSite;

    @Value("${app.auth.access-token-cookie-max-age-minutes}")
    private long accessTokenCookieMinutes;

    @Value("${app.auth.id-token-cookie-max-age-minutes}")
    private long idTokenCookieMinutes;

    @Value("${app.auth.refresh-token-cookie-max-age-days}")
    private long refreshTokenCookieDays;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        var tokens = cognitoAuthService.login(
                request.getEmail(),
                request.getPassword()
        );

        ResponseCookie accessCookie = createCookie(
                "access_token",
                tokens.getAccessToken(),
                Duration.ofMinutes(accessTokenCookieMinutes)
        );

        ResponseCookie idCookie = createCookie(
                "id_token",
                tokens.getIdToken(),
                Duration.ofMinutes(idTokenCookieMinutes)
        );

        ResponseCookie refreshCookie = createCookie(
                "refresh_token",
                tokens.getRefreshToken(),
                Duration.ofDays(refreshTokenCookieDays)
        );

        LoginResponse user = userService.getCurrentUserByEmail(request.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, idCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return unauthorizedAndClearCookies("Missing refresh token");
        }

        try {
            var tokens = cognitoAuthService.refresh(refreshToken);

            ResponseCookie accessCookie = createCookie(
                    "access_token",
                    tokens.getAccessToken(),
                    Duration.ofMinutes(accessTokenCookieMinutes)
            );

            ResponseCookie idCookie = createCookie(
                    "id_token",
                    tokens.getIdToken(),
                    Duration.ofMinutes(idTokenCookieMinutes)
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, idCookie.toString())
                    .body(Map.of("message", "Refresh success"));
        } catch (Exception e) {
            return unauthorizedAndClearCookies("Refresh token invalid or expired");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie accessCookie = clearCookie("access_token");
        ResponseCookie idCookie = clearCookie("id_token");
        ResponseCookie refreshCookie = clearCookie("refresh_token");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, idCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Logout success"));
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return cognitoAuthService.register(request);
    }

    @PostMapping("/confirm-register")
    public ResponseEntity<?> confirmRegister(
            @RequestBody ConfirmRegisterRequest request
    ) {

        cognitoAuthService.confirmRegister(
                request.getEmail(),
                request.getOtp()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Account verified successfully"
                )
        );
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(
            @RequestBody ResendCodeRequest request
    ) {

        cognitoAuthService.resendCode(
                request.getEmail()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Verification code sent successfully"
                )
        );
    }

    private ResponseCookie createCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure) // dev localhost
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }
    private ResponseEntity<?> unauthorizedAndClearCookies(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, clearCookie("access_token").toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie("id_token").toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie("refresh_token").toString())
                .body(Map.of("message", message));
    }
}