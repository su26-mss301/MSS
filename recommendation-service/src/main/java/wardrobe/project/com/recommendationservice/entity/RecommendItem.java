package wardrobe.project.com.recommendationservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recommend_item")
@Data
public class RecommendItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "recommend_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id") // Có thể null nếu gợi ý chung chung không theo event
    private Event event;

    @Column(name = "recommendation_score", nullable = false)
    private Float recommendationScore;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "group_styles", columnDefinition = "TEXT")
    private String groupStyles;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}