package wardrobe.project.com.userservice.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.request.auth.ConfirmRegisterRequest;
import wardrobe.project.com.userservice.dto.request.auth.LoginRequest;
import wardrobe.project.com.userservice.dto.request.auth.RegisterRequest;
import wardrobe.project.com.userservice.dto.request.auth.ResendCodeRequest;
import wardrobe.project.com.userservice.service.auth.CognitoAuthService;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CognitoAuthService cognitoAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var tokens = cognitoAuthService.login(
                request.getEmail(),
                request.getPassword()
        );

        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        ResponseCookie idCookie = ResponseCookie.from("id_token", tokens.getIdToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, idCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "Login success"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        cognitoAuthService.register(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(Map.of("message", "Registration success"));
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
}