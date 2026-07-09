package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import wardrobe.project.com.userservice.enums.FriendGroupJoinRequestStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "friend_group_join_requests")
public class FriendGroupJoinRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", nullable = false, updatable = false)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FriendGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendGroupJoinRequestStatus status;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by")
    private User respondedBy;

    @Column(name = "previously_kicked", nullable = false)
    private boolean previouslyKicked;
}