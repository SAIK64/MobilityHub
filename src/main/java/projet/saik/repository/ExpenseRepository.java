package projet.saik.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import projet.saik.model.Expense;

// Fournit les requêtes de consultation des dépenses de budget.
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    // Liste les dépenses d’un budget de la plus récente à la plus ancienne.
    List<Expense> findByBudgetIdOrderByExpenseDateDesc(Long budgetId);
    // Filtre les dépenses d’un budget par catégorie.
    List<Expense> findByBudgetIdAndCategory(Long budgetId, String category);

    // Additionne les montants par catégorie pour produire une répartition budgétaire.
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.budget.id = :budgetId GROUP BY e.category")
    List<Object[]> sumAmountByCategory(@Param("budgetId") Long budgetId);
}