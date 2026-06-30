package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.entity.FriendGroupMember;
import wardrobe.project.com.userservice.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendGroupMemberRepository extends JpaRepository<FriendGroupMember, String> {

    boolean existsByGroupAndUserAndActiveTrue(FriendGroup group, User user);

    Optional<FriendGroupMember> findByGroupAndUserAndActiveTrue(FriendGroup group, User user);

    List<FriendGroupMember> findByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    long countByGroupAndActiveTrue(FriendGroup group);
}