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
@Table(name = "group_shared_clothing_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSharedClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "share_id", updatable = false, nullable = false)
    private UUID shareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ClothingItem clothingItem;

    /**
     * groupId từ user-service (lưu dạng String, không FK vì khác service)
     */
    @Column(name = "group_id", nullable = false, length = 100)
    private String groupId;

    /**
     * userId của người share — lấy từ header X-Auth-User-Id do API Gateway inject
     */
    @Column(name = "shared_by_user_id", nullable = false)
    private UUID sharedByUserId;

    @CreationTimestamp
    @Column(name = "shared_at", nullable = false, updatable = false)
    private LocalDateTime sharedAt;

    /**
     * Soft-delete: set khi user "unshare"
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
