package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import wardrobe.project.com.userservice.enums.FriendGroupInvitationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "friend_group_invitations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_friend_group_invitation_pending",
                        columnNames = {"group_id", "invitee_id", "status"}
                )
        }
)
public class FriendGroupInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invitation_id", nullable = false, updatable = false)
    private String invitationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FriendGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendGroupInvitationStatus status;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "responded_at")
    private Instant respondedAt;
}