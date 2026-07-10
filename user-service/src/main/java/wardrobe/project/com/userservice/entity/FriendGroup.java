package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "friend_groups")
public class FriendGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_id", nullable = false, updatable = false)
    private String groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String emoji;

    /**
     * Phong cách chủ đạo của nhóm (e.g. ["MINIMAL", "OFFICE"]).
     * Dùng để lọc nhóm theo sở thích và auto-leave khi user đổi style.
     * Nullable — nhóm không bắt buộc phải chọn style (với các nhóm cũ).
     */
    @Column(name = "primary_style", length = 500)
    private String primaryStyle;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Override
    protected void onCreate() {
        super.onCreate();

        if (active == null) {
            active = true;
        }

        if (emoji == null || emoji.isBlank()) {
            emoji = "👗";
        }
    }
}