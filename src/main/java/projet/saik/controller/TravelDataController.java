package projet.saik.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projet.saik.service.BudgetService;
import projet.saik.service.ProfileService;
import projet.saik.service.TicketService;
import projet.saik.service.TripService;
import projet.saik.web.TravelDataDtos.BudgetRequest;
import projet.saik.web.TravelDataDtos.BudgetResponse;
import projet.saik.web.TravelDataDtos.CountryRequest;
import projet.saik.web.TravelDataDtos.ExpenseRequest;
import projet.saik.web.TravelDataDtos.ExpenseResponse;
import projet.saik.web.TravelDataDtos.ProfileRequest;
import projet.saik.web.TravelDataDtos.ProfileResponse;
import projet.saik.web.TravelDataDtos.TicketRequest;
import projet.saik.web.TravelDataDtos.TicketResponse;
import projet.saik.web.TravelDataDtos.TripRequest;
import projet.saik.web.TravelDataDtos.TripResponse;

@RestController
@RequestMapping("/api")
// Expose les routes HTTP qui permettent à l’interface d’utiliser les données de voyage.
public class TravelDataController {

    // Délègue les opérations métier au service plutôt que de les traiter dans les routes.
    private final BudgetService budgetService;
    private final ProfileService profileService;
    private final TicketService ticketService;
    private final TripService tripService;

    // Reçoit le service géré par Spring au démarrage de l’application.
    public TravelDataController(BudgetService budgetService, ProfileService profileService, TicketService ticketService, TripService tripService) {
        // Conserve cette dépendance pour les appels des routes.
        this.budgetService = budgetService;
        this.profileService = profileService;
        this.ticketService = ticketService;
        this.tripService = tripService;
    }

    // Retourne les voyages appartenant à la personne connectée.
    @GetMapping("/trips")
    public List<TripResponse> getTrips() {
        // Le service vérifie l’utilisateur courant et transforme les entités en réponses JSON.
        return tripService.getTrips();
    }

    // Crée un voyage à partir du corps JSON après validation des champs.
    @PostMapping("/trips")
    public TripResponse createTrip(@Valid @RequestBody TripRequest request) {
        // Délègue la création et renvoie le voyage nouvellement enregistré.
        return tripService.createTrip(request);
    }

    // Update un voyage existant avec les données reçues après validation.
    @PutMapping("/trips/{tripId}")
    public TripResponse updateTrip(@PathVariable Long tripId, @Valid @RequestBody TripRequest request) {
        // Délègue la mise à jour et renvoie le voyage modifié pour confirmation.
        return tripService.updateTrip(tripId, request);
    }

    // Supprime un voyage et toutes ses données associées.
    @DeleteMapping("/trips/{tripId}")
    public TripResponse deleteTrip(@PathVariable Long tripId) {
        // Délègue la suppression et renvoie le voyage supprimé pour confirmation.
        return tripService.deleteTrip(tripId);
    }


    @PostMapping("/trips/{tripId}/countries")
    public TripResponse addCountry(@PathVariable Long tripId, @Valid @RequestBody CountryRequest request) {
        return tripService.addCountry(tripId, request);
    }

    @DeleteMapping("/trips/{tripId}/countries/{iso}")
    public TripResponse removeCountry(@PathVariable Long tripId, @PathVariable String iso) {
        return tripService.removeCountry(tripId, iso);
    }

    // Liste les titres de transport rattachés au voyage indiqué.
    @GetMapping("/trips/{tripId}/tickets")
    public List<TicketResponse> getTickets(@PathVariable Long tripId) {
        // Le service vérifie aussi que le voyage appartient à l’utilisateur courant.
        return ticketService.getTickets(tripId);
    }

    // Ajoute un titre de transport à un voyage précis.
    @PostMapping("/trips/{tripId}/tickets")
    public TicketResponse createTicket(
            // Récupère l’identifiant du voyage depuis l’adresse de la route.
            @PathVariable Long tripId,
            // Lit et valide les informations envoyées dans le corps JSON.
            @Valid @RequestBody TicketRequest request) {
        // Le service enregistre le titre et sa dépense de transport éventuelle.
        return ticketService.createTicket(tripId, request);
    }

    // Supprime le titre correspondant à l’identifiant fourni.
    @DeleteMapping("/tickets/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId) {
        // Le service contrôle les droits puis supprime également la dépense automatique associée.
        ticketService.deleteTicket(ticketId);
        // Répond avec le statut HTTP 204, qui indique que la suppression a réussi sans contenu.
        return ResponseEntity.noContent().build();
    }

    // Retourne les budgets d’un voyage.
    @GetMapping("/trips/{tripId}/budget")
    public BudgetResponse getBudget(@PathVariable Long tripId) {
        return budgetService.getBudget(tripId);
    }

    // Définit les budgets par catégorie d’un voyage.
    @PutMapping("/trips/{tripId}/budget")
    public BudgetResponse updateBudget(@PathVariable Long tripId, @Valid @RequestBody BudgetRequest request) {
        return budgetService.updateBudget(tripId, request);
    }

    // Retourne les dépenses d’un voyage.
    @GetMapping("/trips/{tripId}/expenses")
    public List<ExpenseResponse> getExpenses(@PathVariable Long tripId) {
        // Le service filtre les dépenses selon le voyage et l’utilisateur connecté.
        return budgetService.getExpenses(tripId);
    }

    // Ajoute une dépense manuelle ou automatique à un voyage.
    @PostMapping("/trips/{tripId}/expenses")
    public ExpenseResponse createExpense(
            // Identifiant du voyage auquel rattacher la dépense.
            @PathVariable Long tripId,
            // Données reçues et vérifiées avant leur traitement.
            @Valid @RequestBody ExpenseRequest request) {
        // Retourne la dépense enregistrée sous la forme prévue pour l’API.
        return budgetService.createExpense(tripId, request);
    }

    // Supprime une dépense grâce à son identifiant.
    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        // Le service vérifie que la dépense appartient à un voyage de l’utilisateur.
        budgetService.deleteExpense(expenseId);
        // Signale la réussite de la suppression sans renvoyer de corps de réponse.
        return ResponseEntity.noContent().build();
    }

    // Retourne le profil de la personne actuellement connectée.
    @GetMapping("/profile")
    public ProfileResponse getProfile() {
        // Le service charge le compte et construit les données de réponse.
        return profileService.getProfile();
    }

    // Met à jour le profil avec les informations validées reçues en JSON.
    @PutMapping("/profile")
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        // La réponse contient les nouvelles informations enregistrées.
        return profileService.updateProfile(request);
    }
}
