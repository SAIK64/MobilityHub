package projet.saik.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import projet.saik.web.TravelDataDtos.BudgetResponse;
import projet.saik.web.TravelDataDtos.ExpenseResponse;

// Modèle de vue de la page budget : totaux, état de chaque catégorie et données du diagramme.
// Tous les calculs sont faits côté serveur, le template ne fait qu'afficher.
@Getter
public class BudgetView {

    // Catégories proposées dans les formulaires : clé enregistrée en base -> libellé + couleur.
    public static final List<Category> CATEGORIES = List.of(
            new Category("Transport", "Transports", "#1d3312"),
            new Category("Hébergement", "Hébergements", "#4d8c3a"),
            new Category("Formalités", "Formalités & Démarches", "#c28f2c"),
            new Category("Activités", "Activités & Visites", "#3b7ea1"),
            new Category("Restauration", "Vie courante & Restauration", "#c2573b"),
            new Category("Tech", "Télécoms & Tech", "#7a5ea8"));

    private static final String OTHER_COLOR = "#74796e";

    private final double totalBudget;
    private final double totalSpent;
    private final double remaining;
    private final boolean overBudget;
    // Catégories ayant une dépense ou un budget (barres « dépensé / budget »).
    private final List<CategoryStat> categories;
    // Catégories ayant au moins une dépense (diagramme circulaire et légende).
    private final List<CategoryStat> slices;
    // Budget fixé pour chaque catégorie, pour pré-remplir le formulaire.
    private final Map<String, Double> limits;

    public BudgetView(BudgetResponse budget, List<ExpenseResponse> expenses) {
        this.limits = budget.categoryBudgets() == null ? Map.of() : budget.categoryBudgets();

        Map<String, Double> spent = new LinkedHashMap<>();
        for (ExpenseResponse expense : expenses) {
            spent.merge(expense.category(), expense.amount() == null ? 0 : expense.amount(), Double::sum);
        }
        this.totalSpent = spent.values().stream().mapToDouble(Double::doubleValue).sum();
        this.totalBudget = limits.values().stream().mapToDouble(v -> v == null ? 0 : v).sum();
        this.remaining = totalBudget - totalSpent;
        this.overBudget = totalBudget > 0 && totalSpent > totalBudget;

        // Catégories connues puis catégories inconnues (anciennes données) en gris.
        List<Category> all = new ArrayList<>(CATEGORIES);
        for (String key : concat(spent.keySet(), limits.keySet())) {
            if (all.stream().noneMatch(c -> c.key().equals(key))) {
                all.add(new Category(key, key, OTHER_COLOR));
            }
        }

        List<CategoryStat> stats = new ArrayList<>();
        List<CategoryStat> pie = new ArrayList<>();
        double offset = 0;
        for (Category category : all) {
            double amount = spent.getOrDefault(category.key(), 0.0);
            Double limitValue = limits.get(category.key());
            double limit = limitValue == null ? 0 : limitValue;
            if (amount == 0 && limit == 0) {
                continue;
            }
            double share = totalSpent > 0 ? amount / totalSpent * 100 : 0;
            CategoryStat stat = new CategoryStat(category, amount, limit, share, offset);
            stats.add(stat);
            if (amount > 0) {
                pie.add(stat);
                offset += share;
            }
        }
        this.categories = stats;
        this.slices = pie;
    }

    private static List<String> concat(java.util.Collection<String> a, java.util.Collection<String> b) {
        List<String> keys = new ArrayList<>(a);
        keys.addAll(b);
        return keys;
    }

    public record Category(String key, String label, String color) {
        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getColor() { return color; }
    }

    // Une catégorie avec ses montants et les valeurs utiles au dessin (barre, part du diagramme).
    @Getter
    public static class CategoryStat {
        private final String key;
        private final String label;
        private final String color;
        private final double spent;
        private final double limit;
        private final boolean over;
        private final double share;
        private final double dashOffset;

        CategoryStat(Category category, double spent, double limit, double share, double offset) {
            this.key = category.key();
            this.label = category.label();
            this.color = over(spent, limit) ? "#ba1a1a" : category.color();
            this.spent = spent;
            this.limit = limit;
            this.over = over(spent, limit);
            this.share = share;
            this.dashOffset = -offset;
        }

        private static boolean over(double spent, double limit) {
            return limit > 0 && spent > limit;
        }

        // Largeur de la barre en % : dépensé / budget, plafonnée à 100.
        public double getBarWidth() {
            if (limit > 0) return Math.min(100, spent / limit * 100);
            return spent > 0 ? 100 : 0;
        }

        // Couleur de la part dans le diagramme (toujours la couleur de la catégorie).
        public String getSliceColor() {
            return CATEGORIES.stream().filter(c -> c.key().equals(key)).map(Category::color).findFirst().orElse(OTHER_COLOR);
        }
    }
}
