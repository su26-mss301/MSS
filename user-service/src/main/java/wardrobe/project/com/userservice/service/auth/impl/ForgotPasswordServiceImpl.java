package wardrobe.project.com.userservice.service.auth.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.dto.request.auth.ForgotPasswordRequest;
import wardrobe.project.com.userservice.dto.request.auth.VerifyForgotPasswordOtpRequest;
import wardrobe.project.com.userservice.dto.response.auth.ResetPasswordRequest;
import wardrobe.project.com.userservice.dto.response.auth.VerifyForgotPasswordOtpResponse;
import wardrobe.project.com.userservice.entity.PasswordResetOtp;
import wardrobe.project.com.userservice.repository.PasswordResetOtpRepository;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.auth.EmailService;
import wardrobe.project.com.userservice.service.auth.ForgotPasswordService;
import wardrobe.project.com.userservice.service.keycloak.KeycloakUserService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class ForgotPasswordServiceImpl implements ForgotPasswordService {
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final KeycloakUserService keycloakUserService;

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        boolean exists = userRepository.existsByEmail(email)
                || keycloakUserService.existsByEmail(email);

        if (!exists) {
            // Không nên báo email không tồn tại để tránh bị dò tài khoản
            return;
        }

        String otp = generateOtp();

        PasswordResetOtp resetOtp = passwordResetOtpRepository.findByEmail(email)
                .orElse(new PasswordResetOtp());

        resetOtp.setEmail(email);
        resetOtp.setOtpCode(otp);
        resetOtp.setOtpExpiredAt(LocalDateTime.now().plusMinutes(5));
        resetOtp.setAttemptCount(0);
        resetOtp.setVerified(false);
        resetOtp.setResetToken(null);
        resetOtp.setResetTokenExpiredAt(null);

        if (resetOtp.getCreatedAt() == null) {
            resetOtp.setCreatedAt(LocalDateTime.now());
        }

        passwordResetOtpRepository.save(resetOtp);

        emailService.sendForgotPasswordOtp(email, otp);
    }

    public VerifyForgotPasswordOtpResponse verifyOtp(VerifyForgotPasswordOtpRequest request) {
        String email = normalizeEmail(request.getEmail());

        PasswordResetOtp resetOtp = passwordResetOtpRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (resetOtp.getOtpExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (resetOtp.getAttemptCount() >= 5) {
            throw new RuntimeException("Too many OTP attempts");
        }

        if (!resetOtp.getOtpCode().equals(request.getOtp())) {
            resetOtp.setAttemptCount(resetOtp.getAttemptCount() + 1);
            passwordResetOtpRepository.save(resetOtp);
            throw new RuntimeException("Invalid OTP");
        }

        String resetToken = UUID.randomUUID().toString();

        resetOtp.setVerified(true);
        resetOtp.setResetToken(resetToken);
        resetOtp.setResetTokenExpiredAt(LocalDateTime.now().plusMinutes(10));

        passwordResetOtpRepository.save(resetOtp);

        return new VerifyForgotPasswordOtpResponse(resetToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetOtp resetOtp = passwordResetOtpRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (!resetOtp.isVerified()) {
            throw new RuntimeException("OTP has not been verified");
        }

        if (resetOtp.getResetTokenExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token expired");
        }

        keycloakUserService.resetPasswordByEmail(
                resetOtp.getEmail(),
                request.getNewPassword()
        );

        passwordResetOtpRepository.delete(resetOtp);
    }

    private String generateOtp() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(otp);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
