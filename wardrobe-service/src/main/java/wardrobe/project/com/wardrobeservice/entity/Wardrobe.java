package wardrobe.project.com.wardrobeservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wardrobe")
@SQLRestriction("deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wardrobe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wardrobe_id", updatable = false, nullable = false)
    private UUID wardrobeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "wardrobe_name", nullable = false, length = 100)
    private String wardrobeName;

    @OneToMany(mappedBy = "wardrobe", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private java.util.List<WardrobeZone> zones;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
