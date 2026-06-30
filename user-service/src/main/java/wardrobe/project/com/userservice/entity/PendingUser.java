package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "pending_users",
        indexes = {
                @Index(name = "idx_pending_users_email", columnList = "email")
        }
)
public class PendingUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pending_user_id")
    private String id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /**
     * Mật khẩu chỉ lưu tạm trước khi verify OTP.
     * Sau khi tạo user trong Keycloak thì xóa record này.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "otp_expired_at", nullable = false)
    private LocalDateTime otpExpiredAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}