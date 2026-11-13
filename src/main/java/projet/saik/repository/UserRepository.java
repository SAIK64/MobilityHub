package projet.saik.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projet.saik.model.User;

import java.util.Optional;

// Fournit les opérations de persistance et de recherche des comptes utilisateurs.
public interface UserRepository extends JpaRepository<User, Long> {
    // Recherche une adresse e-mail en respectant exactement sa casse.
    Optional<User> findByEmail(String email);
    // Recherche une adresse sans distinguer majuscules et minuscules.
    Optional<User> findByEmailIgnoreCase(String email);
    // Vérifie rapidement si une adresse est déjà utilisée.
    boolean existsByEmail(String email);
}