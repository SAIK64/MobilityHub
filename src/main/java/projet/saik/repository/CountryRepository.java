package projet.saik.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import projet.saik.model.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByCodeIso(String codeIso);
    List<Country> findAllByOrderByNameAsc();

    @Query("SELECT COUNT(DISTINCT c.id) FROM Trip t JOIN t.countries c WHERE t.user.id = :userId AND t.endDate < :today")
    long countVisitedCountriesByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);
}