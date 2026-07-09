package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.entity.FriendGroupInvitation;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.FriendGroupInvitationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendGroupInvitationRepository
        extends JpaRepository<FriendGroupInvitation, String> {

    boolean existsByGroupAndInviteeAndStatus(
            FriendGroup group,
            User invitee,
            FriendGroupInvitationStatus status
    );

    Optional<FriendGroupInvitation> findByInvitationIdAndInviteeAndStatus(
            String invitationId,
            User invitee,
            FriendGroupInvitationStatus status
    );

    Optional<FriendGroupInvitation> findByInvitationIdAndGroupAndStatus(
            String invitationId,
            FriendGroup group,
            FriendGroupInvitationStatus status
    );

    List<FriendGroupInvitation> findAllByInviteeAndStatusOrderByCreatedAtDesc(
            User invitee,
            FriendGroupInvitationStatus status
    );

    List<FriendGroupInvitation> findAllByGroupAndStatusOrderByCreatedAtDesc(
            FriendGroup group,
            FriendGroupInvitationStatus status
    );
}