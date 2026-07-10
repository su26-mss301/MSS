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
@Table(
    name = "shared_clothing_item_like",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_like_share_user",
            columnNames = {"share_id", "user_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedClothingItemLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "like_id", updatable = false, nullable = false)
    private UUID likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id", nullable = false)
    private GroupSharedClothingItem sharedItem;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "liked_at", nullable = false, updatable = false)
    private LocalDateTime likedAt;
}
