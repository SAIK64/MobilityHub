package projet.saik.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "budgets")
// Entité qui relie un voyage à ses dépenses et calcule les totaux du budget.
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    // Montant prévisionnel saisi pour comparer le budget prévu au budget dépensé.
    private Double plannedTotal = 0.0;

    // Un budget appartient à un seul voyage.
    @OneToOne
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    // Les dépenses sont supprimées avec le budget lorsqu’il est supprimé.
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    public Budget() {}

    // Ce total est calculé à la demande et n’est pas enregistré dans une colonne.
    @Transient
    public Double getActualTotal() {
        // Une collection absente équivaut à un budget sans dépense.
        if (expenses == null) return 0.0;
        // Ignore les montants absents puis additionne les autres.
        return expenses.stream()
                .filter(e -> e.getAmount() != null)
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    // Calcule l’écart entre le montant prévisionnel et les dépenses réelles.
    @Transient
    public Double getRemainingForecast() {
        double planned = plannedTotal != null ? plannedTotal : 0.0;
        return planned - getActualTotal();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPlannedTotal() { return plannedTotal; }
    public void setPlannedTotal(Double plannedTotal) { this.plannedTotal = plannedTotal; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }
}