package projet.saik.model;

import java.util.ArrayList;
import java.util.List;

//utilisé par hibernate pour traduire la classe JAVA en table SQL H2
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

//indique à Hibernate que la classe est persistance c'est à dire que chaque instance de la classe correspond à une ligne dans la table SQL
@Entity
//force nom de la table SQL à "budgets" au lieu de "budget" pour éviter conflit avec H2
@Table(name = "budgets")
//public pour que Hibernate puisse instancier la classe
public class Budget {

    //clé primaire 
    @Id
    //indique à Hibernate que la valeur de l'identifiant est générée automatiquement par la base de données
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //utilise Long pour que id soit null par défaut et non 0, ce qui permet de savoir si l'objet a déjà été enregistré en base de données ou non
    private Long id;

    //indique à Hibernate que la colonne est obligatoire et ne peut pas être null
    @Column(nullable = false)
    // Montant prévisionnel saisi pour comparer le budget prévu au budget dépensé.
    private Double plannedTotal = 0.0;

    //Déclare relation OneToOne avec Trip, avec jointure sur la colonne trip_id, qui est obligatoire et unique
    //un Budget est associé à un seul Trip, et un Trip est associé à un seul Budget
    @OneToOne
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    //Un Budget peut avoir plusieurs Expenses, mais une Expense est associée à un seul Budget
    //cascade = CascadeType.ALL pour que les opérations sur Budget soient propagées aux Expenses associées
    //orphanRemoval = true pour que les Expenses orphelines soient supprimées de la base de données
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    //Constructeur 
    public Budget() {}

    //indique à Hibernate que la méthode getActualTotal() ne correspond pas à une colonne de la table SQL, mais est calculée à partir des Expenses associées
    //getActualTotal() retourne la somme des montants des Expenses associées au Budget, en filtrant les Expenses avec montant non null
    @Transient
    public Double getActualTotal() {
        // Une collection absente équivaut à un budget sans dépense.
        if (expenses == null) return 0.0;
        //utilise stream pour filtrer les Expenses avec montant non null, puis somme les montants
        return expenses.stream()
                .filter(e -> e.getAmount() != null)
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    //getRemainingForecast() retourne la différence entre le montant total prévu et le montant total réel des Expenses associées au Budget
    @Transient
    public Double getRemainingForecast() {
        double planned = plannedTotal != null ? plannedTotal : 0.0;
        return planned - getActualTotal();
    }

    // getter et setter 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPlannedTotal() { return plannedTotal; }
    public void setPlannedTotal(Double plannedTotal) { this.plannedTotal = plannedTotal; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    //recuperer la liste des dépenses associées au budget
    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }
}