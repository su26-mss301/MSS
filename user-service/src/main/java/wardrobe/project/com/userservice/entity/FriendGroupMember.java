package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import wardrobe.project.com.userservice.enums.FriendGroupMemberStatus;
import wardrobe.project.com.userservice.enums.FriendGroupRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "friend_group_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_friend_group_member",
                        columnNames = {"group_id", "user_id"}
                )
        }
)
public class FriendGroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "member_id", nullable = false, updatable = false)
    private String memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FriendGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendGroupRole role;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendGroupMemberStatus status;

    @Override
    protected void onCreate() {
        super.onCreate();

        if (active == null) {
            active = true;
        }

        if (role == null) {
            role = FriendGroupRole.MEMBER;
        }
    }
}