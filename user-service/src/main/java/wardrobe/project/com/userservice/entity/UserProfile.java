package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import wardrobe.project.com.userservice.enums.Gender;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id", nullable = false, updatable = false)
    private String profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "body_measurement", columnDefinition = "TEXT")
    private String bodyMeasurement;

    @Column(name = "favorite_colors", columnDefinition = "TEXT")
    private String favoriteColors;

    @Column(name = "style_preference", length = 100)
    private String stylePreference;

    @Column(name = "lifestyle_preference", length = 100)
    private String lifestylePreference;

    @Column(length = 100)
    private String occupation;

    @Column(columnDefinition = "TEXT")
    private String note;
}