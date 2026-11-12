package projet.saik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projet.saik.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Utilisateur, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
