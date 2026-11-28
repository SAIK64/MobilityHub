package projet.saik.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import projet.saik.model.Trip;

// Fournit les requêtes de voyage, en particulier celles limitées à leur propriétaire.
public interface TripRepository extends JpaRepository<Trip, Long> {
    // Liste les voyages d’un compte par date de départ croissante.
    List<Trip> findByUserIdOrderByStartDateAsc(Long userId);
    // Charge un voyage seulement si son identifiant et son propriétaire correspondent.
    Optional<Trip> findByIdAndUserId(Long id, Long userId);

    // Recherche les voyages d’un compte dont le nom, le nom d’un pays ou le code ISO d’un pays
    // contient le texte demandé (sans tenir compte de la casse). La jointure parcourt la relation N-N Trip <-> Country.
    @Query("""
            SELECT DISTINCT t FROM Trip t LEFT JOIN t.countries c
            WHERE t.user.id = :userId
              AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(c.codeIso) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY t.startDate ASC
            """)
    List<Trip> searchByUser(@Param("userId") Long userId, @Param("query") String query);
}