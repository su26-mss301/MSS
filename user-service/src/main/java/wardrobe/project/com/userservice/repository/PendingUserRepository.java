package wardrobe.project.com.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import wardrobe.project.com.userservice.entity.PendingUser;

import java.util.Optional;

@Repository
public interface PendingUserRepository extends JpaRepository<PendingUser, String> {
    Optional<PendingUser> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);
}
