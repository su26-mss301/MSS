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
@Table(name = "clothing_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id", updatable = false, nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private WardrobeZone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "image_id")
    private UUID imageId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "dominant_color")
    private String dominantColor;

    @Column(name = "style")
    private String style;

    @Column(name = "confidence_score")
    private Float confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
