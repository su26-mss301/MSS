package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.entity.UserProfile;

import java.util.Optional;


@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUser(User user);
}
