package wardrobe.project.com.userservice.service.auth.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import wardrobe.project.com.userservice.dto.request.auth.LoginRequest;
import wardrobe.project.com.userservice.dto.request.auth.RegisterRequest;
import wardrobe.project.com.userservice.dto.request.auth.VerifyRegisterOtpRequest;
import wardrobe.project.com.userservice.dto.request.user.CreateUserRequest;
import wardrobe.project.com.userservice.dto.response.auth.KeycloakTokenResponse;
import wardrobe.project.com.userservice.entity.PendingUser;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;
import wardrobe.project.com.userservice.repository.PendingUserRepository;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.auth.AuthService;
import wardrobe.project.com.userservice.service.auth.EmailService;
import wardrobe.project.com.userservice.service.keycloak.KeycloakUserService;

import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final KeycloakUserService keycloakUserService;

    private final RestTemplate restTemplate;

    @Value("${server.servlet.keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${server.servlet.keycloak.app-realm}")
    private String keycloakRealm;

    @Value("${server.servlet.keycloak.client-id}")
    private String keycloakClientId;

    @Value("${server.servlet.keycloak.client-secret:}")
    private String keycloakClientSecret;

    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String username = request.getUsername().trim();

        if (userRepository.existsByEmail(email) || keycloakUserService.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        String otp = generateOtp();

        PendingUser pendingUser = pendingUserRepository.findByEmail(email)
                .orElse(new PendingUser());

        pendingUser.setEmail(email);
        pendingUser.setUsername(username);
        pendingUser.setPassword(request.getPassword());
        pendingUser.setFullName(request.getFullName());
        pendingUser.setOtpCode(otp);
        pendingUser.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));
        pendingUser.setAttemptCount(0);

        if (pendingUser.getCreatedAt() == null) {
            pendingUser.setCreatedAt(LocalDateTime.now());
        }

        pendingUserRepository.save(pendingUser);

        emailService.sendRegisterOtp(email, otp);
    }

    public void verifyRegisterOtp(VerifyRegisterOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        PendingUser pendingUser = pendingUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Pending user not found"));

        if (pendingUser.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (pendingUser.getAttemptCount() >= 5) {
            throw new RuntimeException("Too many OTP attempts");
        }

        if (!pendingUser.getOtpCode().equals(request.getOtp())) {
            pendingUser.setAttemptCount(pendingUser.getAttemptCount() + 1);
            pendingUserRepository.save(pendingUser);
            throw new RuntimeException("Invalid OTP");
        }

        if (userRepository.existsByEmail(email) || keycloakUserService.existsByEmail(email)) {
            pendingUserRepository.delete(pendingUser);
            throw new RuntimeException("Email already exists");
        }

        String keycloakUserId = keycloakUserService.createUser(
                CreateUserRequest.builder()
                        .email(pendingUser.getEmail())
                        .username(pendingUser.getUsername())
                        .password(pendingUser.getPassword())
                        .build()
        );

        User user = User.builder()
                .userId(keycloakUserId)
                .email(pendingUser.getEmail())
                .username(pendingUser.getUsername())
                .fullName(pendingUser.getFullName())
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);

        pendingUserRepository.delete(pendingUser);
    }

    @Override
    public void resendCode(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail) || keycloakUserService.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Account already verified");
        }

        PendingUser pendingUser = pendingUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Pending user not found. Please register again"));

        String newOtp = generateOtp();

        pendingUser.setOtpCode(newOtp);
        pendingUser.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));
        pendingUser.setAttemptCount(0);

        pendingUserRepository.save(pendingUser);

        emailService.sendRegisterOtp(normalizedEmail, newOtp);
    }

    public KeycloakTokenResponse login(LoginRequest request) {
        String tokenUrl = keycloakServerUrl
                + "/realms/"
                + keycloakRealm
                + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakClientId);
        body.add("username", request.getEmail());
        body.add("password", request.getPassword());

        if (StringUtils.hasText(keycloakClientSecret)) {
            body.add("client_secret", keycloakClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    restTemplate.postForEntity(
                            tokenUrl,
                            requestEntity,
                            KeycloakTokenResponse.class
                    );

            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException("Invalid email or password");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new RuntimeException("Login failed: " + e.getResponseBodyAsString());
        }
    }

    @Override
    public void logout(String refreshToken) {
        String logoutUrl = keycloakServerUrl
                + "/realms/"
                + keycloakRealm
                + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", keycloakClientId);
        body.add("refresh_token", refreshToken);

        if (StringUtils.hasText(keycloakClientSecret)) {
            body.add("client_secret", keycloakClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(logoutUrl, requestEntity, String.class);
    }

    @Override
    public KeycloakTokenResponse refresh(String refreshToken) {
        String tokenUrl = keycloakServerUrl
                + "/realms/"
                + keycloakRealm
                + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", keycloakClientId);
        body.add("refresh_token", refreshToken);

        if (StringUtils.hasText(keycloakClientSecret)) {
            body.add("client_secret", keycloakClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    restTemplate.postForEntity(
                            tokenUrl,
                            requestEntity,
                            KeycloakTokenResponse.class
                    );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Refresh token invalid or expired");
        }
    }

    @Override
    public KeycloakTokenResponse exchangeAuthorizationCode(
            String code,
            String redirectUri
    ) {
        String tokenUrl = keycloakServerUrl
                + "/realms/"
                + keycloakRealm
                + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("grant_type", "authorization_code");
        body.add("client_id", keycloakClientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        if (StringUtils.hasText(keycloakClientSecret)) {
            body.add("client_secret", keycloakClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KeycloakTokenResponse> response =
                    restTemplate.postForEntity(
                            tokenUrl,
                            requestEntity,
                            KeycloakTokenResponse.class
                    );

            if (response.getBody() == null) {
                throw new RuntimeException("Keycloak returned empty token response");
            }

            return response.getBody();

        } catch (HttpClientErrorException e) {
            System.err.println("KEYCLOAK STATUS = " + e.getStatusCode());
            System.err.println("KEYCLOAK RESPONSE = " + e.getResponseBodyAsString());

            throw new RuntimeException(
                    "Google login callback failed: "
                            + e.getResponseBodyAsString()
            );
        }
    }

    private String generateOtp() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(otp);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}