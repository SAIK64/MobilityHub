package projet.saik.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import projet.saik.model.Country;

// Fournit les requêtes nécessaires au catalogue des pays.
public interface CountryRepository extends JpaRepository<Country, Long> {
    // Recherche un pays à partir de son code ISO.
    Optional<Country> findByCodeIso(String codeIso);
    // Retourne le catalogue dans l’ordre alphabétique.
    List<Country> findAllByOrderByNameAsc();

    // Compte les pays distincts des voyages terminés de l’utilisateur.
    @Query("SELECT COUNT(DISTINCT c.id) FROM Trip t JOIN t.countries c WHERE t.user.id = :userId AND t.endDate < :today")
    long countVisitedCountriesByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);
}