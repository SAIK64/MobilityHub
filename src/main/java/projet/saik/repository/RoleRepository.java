package projet.saik.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Role;

// Fournit les opérations de base et la recherche des rôles de sécurité.
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Recherche un rôle, par exemple ROLE_USER, à partir de son nom.
    Optional<Role> findByName(String name);
}