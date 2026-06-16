package wardrobe.project.com.userservice.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_username", columnList = "username")
        }
)
public class User extends BaseEntity {

    /**
     * Khóa chính lấy từ Cognito sub.
     */
    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(unique = true, length = 100)
    private String username;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Override
    protected void onCreate() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
    }
}