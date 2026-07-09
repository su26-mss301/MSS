package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_otps")
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reset_otp_id", nullable = false, updatable = false)
    private String resetOtpId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "otp_expired_at", nullable = false)
    private LocalDateTime otpExpiredAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expired_at")
    private LocalDateTime resetTokenExpiredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}