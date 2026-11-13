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

import projet.saik.service.TravelDataService;
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
    private final TravelDataService travelDataService;

    // Reçoit le service géré par Spring au démarrage de l’application.
    public TravelDataController(TravelDataService travelDataService) {
        // Conserve cette dépendance pour les appels des routes.
        this.travelDataService = travelDataService;
    }

    // Retourne les voyages appartenant à la personne connectée.
    @GetMapping("/trips")
    public List<TripResponse> getTrips() {
        // Le service vérifie l’utilisateur courant et transforme les entités en réponses JSON.
        return travelDataService.getTrips();
    }

    // Crée un voyage à partir du corps JSON après validation des champs.
    @PostMapping("/trips")
    public TripResponse createTrip(@Valid @RequestBody TripRequest request) {
        // Délègue la création et renvoie le voyage nouvellement enregistré.
        return travelDataService.createTrip(request);
    }

    // Liste les titres de transport rattachés au voyage indiqué.
    @GetMapping("/trips/{tripId}/tickets")
    public List<TicketResponse> getTickets(@PathVariable Long tripId) {
        // Le service vérifie aussi que le voyage appartient à l’utilisateur courant.
        return travelDataService.getTickets(tripId);
    }

    // Ajoute un titre de transport à un voyage précis.
    @PostMapping("/trips/{tripId}/tickets")
    public TicketResponse createTicket(
            // Récupère l’identifiant du voyage depuis l’adresse de la route.
            @PathVariable Long tripId,
            // Lit et valide les informations envoyées dans le corps JSON.
            @Valid @RequestBody TicketRequest request) {
        // Le service enregistre le titre et sa dépense de transport éventuelle.
        return travelDataService.createTicket(tripId, request);
    }

    // Supprime le titre correspondant à l’identifiant fourni.
    @DeleteMapping("/tickets/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId) {
        // Le service contrôle les droits puis supprime également la dépense automatique associée.
        travelDataService.deleteTicket(ticketId);
        // Répond avec le statut HTTP 204, qui indique que la suppression a réussi sans contenu.
        return ResponseEntity.noContent().build();
    }

    // Retourne les dépenses d’un voyage.
    @GetMapping("/trips/{tripId}/expenses")
    public List<ExpenseResponse> getExpenses(@PathVariable Long tripId) {
        // Le service filtre les dépenses selon le voyage et l’utilisateur connecté.
        return travelDataService.getExpenses(tripId);
    }

    // Ajoute une dépense manuelle ou automatique à un voyage.
    @PostMapping("/trips/{tripId}/expenses")
    public ExpenseResponse createExpense(
            // Identifiant du voyage auquel rattacher la dépense.
            @PathVariable Long tripId,
            // Données reçues et vérifiées avant leur traitement.
            @Valid @RequestBody ExpenseRequest request) {
        // Retourne la dépense enregistrée sous la forme prévue pour l’API.
        return travelDataService.createExpense(tripId, request);
    }

    // Supprime une dépense grâce à son identifiant.
    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        // Le service vérifie que la dépense appartient à un voyage de l’utilisateur.
        travelDataService.deleteExpense(expenseId);
        // Signale la réussite de la suppression sans renvoyer de corps de réponse.
        return ResponseEntity.noContent().build();
    }

    // Retourne le profil de la personne actuellement connectée.
    @GetMapping("/profile")
    public ProfileResponse getProfile() {
        // Le service charge le compte et construit les données de réponse.
        return travelDataService.getProfile();
    }

    // Met à jour le profil avec les informations validées reçues en JSON.
    @PutMapping("/profile")
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest request) {
        // La réponse contient les nouvelles informations enregistrées.
        return travelDataService.updateProfile(request);
    }
}
