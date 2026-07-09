package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;

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
     * Khóa chính lấy từ Keycloak sub.
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

    @Column(name =  "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Override
    protected void onCreate() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }

        if (role == null) {
            role = Role.ROLE_USER;
        }
    }
}