package projet.saik.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}