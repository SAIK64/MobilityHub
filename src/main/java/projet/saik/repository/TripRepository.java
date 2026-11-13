package projet.saik.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Trip;

// Fournit les requêtes de voyage, en particulier celles limitées à leur propriétaire.
public interface TripRepository extends JpaRepository<Trip, Long> {
    // Liste les voyages d’un compte par date de départ croissante.
    List<Trip> findByUserIdOrderByStartDateAsc(Long userId);
    // Charge un voyage seulement si son identifiant et son propriétaire correspondent.
    Optional<Trip> findByIdAndUserId(Long id, Long userId);
}