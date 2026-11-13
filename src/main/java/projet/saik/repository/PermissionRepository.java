package projet.saik.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Permission;

// Fournit les opérations de base et la recherche des autorisations par nom.
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    // Recherche une permission à partir de son identifiant textuel.
    Optional<Permission> findByName(String name);
}