package projet.saik.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByTripId(Long tripId);
}