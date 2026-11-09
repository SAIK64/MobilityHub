package projet.saik.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import projet.saik.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByBudgetIdOrderByExpenseDateDesc(Long budgetId);
    List<Expense> findByBudgetIdAndCategory(Long budgetId, String category);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.budget.id = :budgetId GROUP BY e.category")
    List<Object[]> sumAmountByCategory(@Param("budgetId") Long budgetId);
}