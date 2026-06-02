package wardrobe.project.com.wardrobeservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wardrobe")
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

    @Column(name = "wardrobe_name", nullable = false)
    private String wardrobeName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
