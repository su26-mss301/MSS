package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.entity.User;

import java.util.List;

@Repository
public interface FriendGroupRepository extends JpaRepository<FriendGroup, String> {

    List<FriendGroup> findByOwnerAndActiveTrueOrderByCreatedAtDesc(User owner);

    List<FriendGroup> findByActiveTrueOrderByCreatedAtDesc();
}