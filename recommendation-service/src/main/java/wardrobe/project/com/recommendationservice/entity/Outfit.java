package wardrobe.project.com.recommendationservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outfit")
@Data
public class Outfit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "outfit_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "outfit_name", nullable = false)
    private String outfitName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}