package wardrobe.project.com.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.entity.UserStylePreference;

import java.util.Optional;

public interface UserStylePreferenceRepository extends JpaRepository<UserStylePreference, String> {

    Optional<UserStylePreference> findByUser(User user);
}