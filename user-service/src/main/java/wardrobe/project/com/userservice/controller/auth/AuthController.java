package wardrobe.project.com.userservice.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.request.auth.*;
import wardrobe.project.com.userservice.dto.response.auth.KeycloakTokenResponse;
import wardrobe.project.com.userservice.dto.response.auth.LoginResponse;
import wardrobe.project.com.userservice.dto.response.auth.ResetPasswordRequest;
import wardrobe.project.com.userservice.dto.response.auth.VerifyForgotPasswordOtpResponse;
import wardrobe.project.com.userservice.service.auth.AuthService;
import wardrobe.project.com.userservice.service.auth.ForgotPasswordService;
import wardrobe.project.com.userservice.service.user.UserService;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final ForgotPasswordService forgotPasswordService;


    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String sameSite;

    @Value("${app.auth.access-token-cookie-max-age-minutes}")
    private long accessTokenCookieMinutes;

    @Value("${app.auth.refresh-token-cookie-max-age-days}")
    private long refreshTokenCookieDays;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);

        return ResponseEntity.ok(Map.of(
                "message", "OTP has been sent to your email"
        ));
    }

    @PostMapping("/confirm-register")
    public ResponseEntity<?> confirmRegister(
            @RequestBody VerifyRegisterOtpRequest request
    ) {
        authService.verifyRegisterOtp(request);

        return ResponseEntity.ok(Map.of(
                "message", "Account verified successfully"
        ));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(
            @RequestParam String email
    ) {
        authService.resendCode(email);

        return ResponseEntity.ok(Map.of(
                "message", "Verification code sent successfully"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        var tokens = authService.login(request);

        ResponseCookie accessCookie = createCookie(
                "access_token",
                tokens.getAccessToken(),
                getAccessTokenMaxAge(tokens)
        );

        ResponseCookie refreshCookie = createCookie(
                "refresh_token",
                tokens.getRefreshToken(),
                getRefreshTokenMaxAge(tokens)
        );

        LoginResponse user = userService.getCurrentUserByEmail(request.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
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
            var tokens = authService.refresh(refreshToken);

            ResponseCookie accessCookie = createCookie(
                    "access_token",
                    tokens.getAccessToken(),
                    getAccessTokenMaxAge(tokens)
            );

            ResponseCookie refreshCookie = createCookie(
                    "refresh_token",
                    tokens.getRefreshToken(),
                    getRefreshTokenMaxAge(tokens)
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(Map.of("message", "Refresh success"));
        } catch (Exception e) {
            return unauthorizedAndClearCookies("Refresh token invalid or expired");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                authService.logout(refreshToken);
            } catch (Exception ignored) {
                // Dù Keycloak logout lỗi thì vẫn clear cookie phía client
            }
        }

        ResponseCookie accessCookie = clearCookie("access_token");
        ResponseCookie refreshCookie = clearCookie("refresh_token");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Logout success"));
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
                .secure(cookieSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        forgotPasswordService.forgotPassword(request);

        return ApiResponse.success(Map.of(
                "message", "OTP has been sent to your email"
        ));
    }

    @PostMapping("/verify-forgot-password-otp")
    public ApiResponse<VerifyForgotPasswordOtpResponse> verifyForgotPasswordOtp(
            @RequestBody VerifyForgotPasswordOtpRequest request
    ) {
        return ApiResponse.success(forgotPasswordService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(request);

        return ApiResponse.success(Map.of(
                "message", "Password reset successfully"
        ));
    }

    private ResponseEntity<?> unauthorizedAndClearCookies(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, clearCookie("access_token").toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie("refresh_token").toString())
                .body(Map.of("message", message));
    }

    private Duration getAccessTokenMaxAge(KeycloakTokenResponse tokens) {
        return tokens.getExpiresIn() != null
                ? Duration.ofSeconds(tokens.getExpiresIn())
                : Duration.ofMinutes(accessTokenCookieMinutes);
    }

    private Duration getRefreshTokenMaxAge(KeycloakTokenResponse tokens) {
        return tokens.getRefreshExpiresIn() != null
                ? Duration.ofSeconds(tokens.getRefreshExpiresIn())
                : Duration.ofDays(refreshTokenCookieDays);
    }
}