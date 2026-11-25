package projet.saik.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Budget;

// Fournit les requêtes de lecture et d’écriture sur les budgets.
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    // Recherche le budget associé à un voyage.
    Optional<Budget> findByTripId(Long tripId);
}