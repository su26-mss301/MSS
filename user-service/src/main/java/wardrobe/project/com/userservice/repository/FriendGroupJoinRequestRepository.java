package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.entity.FriendGroupJoinRequest;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.FriendGroupJoinRequestStatus;

import java.util.List;
import java.util.Optional;

public interface FriendGroupJoinRequestRepository
        extends JpaRepository<FriendGroupJoinRequest, String> {

    boolean existsByGroupAndRequesterAndStatus(
            FriendGroup group,
            User requester,
            FriendGroupJoinRequestStatus status
    );

    Optional<FriendGroupJoinRequest> findByRequestIdAndStatus(
            String requestId,
            FriendGroupJoinRequestStatus status
    );

    Optional<FriendGroupJoinRequest> findByRequestIdAndGroupAndStatus(
            String requestId,
            FriendGroup group,
            FriendGroupJoinRequestStatus status
    );

    List<FriendGroupJoinRequest> findAllByGroupAndStatusOrderByCreatedAtDesc(
            FriendGroup group,
            FriendGroupJoinRequestStatus status
    );

    List<FriendGroupJoinRequest> findAllByRequesterAndStatusOrderByCreatedAtDesc(
            User requester,
            FriendGroupJoinRequestStatus status
    );
}