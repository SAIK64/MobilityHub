package projet.saik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import projet.saik.repository.BudgetRepository;
import projet.saik.repository.ExpenseRepository;
import projet.saik.repository.TransportTicketRepository;
import projet.saik.repository.TripRepository;
import projet.saik.service.TravelDataService;
import projet.saik.service.AccountService;
import projet.saik.web.TravelDataDtos.BudgetRequest;
import projet.saik.web.TravelDataDtos.ExpenseRequest;
import projet.saik.web.TravelDataDtos.ProfileRequest;
import projet.saik.web.TravelDataDtos.TicketRequest;
import projet.saik.web.TravelDataDtos.TripRequest;

@SpringBootTest(properties = {
        // Utilise une base mémoire isolée pour que le test ne touche pas aux données locales.
        "spring.datasource.url=jdbc:h2:mem:saik-api-test;DB_CLOSE_DELAY=-1",
        // Crée puis supprime les tables de test automatiquement.
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
// Vérifie le comportement du service avec une vraie base de données de test.
class TravelDataServiceTests {

    @Autowired
    private TravelDataService travelDataService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TransportTicketRepository ticketRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private AccountService accountService;

    @BeforeEach
    // Crée un compte puis place son identifiant dans le contexte de sécurité pour le test.
    void authenticate() {
        var user = accountService.register("etudiant@example.test", "motdepasse-test", "Étudiant", "Test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user.getId().toString(), null, java.util.List.of()));
    }

    @AfterEach
    // Nettoie le contexte pour que l’authentification ne déborde pas sur un autre test.
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    // Vérifie l’enregistrement et la lecture des budgets par catégorie.
    void storesCategoryBudgets() {
        var trip = travelDataService.createTrip(new TripRequest(
                "Budget test", "France", "FR",
                LocalDate.of(2030, 4, 10), LocalDate.of(2030, 4, 20)));
        travelDataService.updateBudget(trip.id(), new BudgetRequest(null, java.util.Map.of("Transport", 500.0, "Tech", 40.0)));
        var budget = travelDataService.getBudget(trip.id());
        assertEquals(500.0, budget.categoryBudgets().get("Transport"));
        assertEquals(2, budget.categoryBudgets().size());
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> travelDataService.updateBudget(trip.id(), new BudgetRequest(null, java.util.Map.of("Tech", -1.0))));
    }

    @Test
    // Vérifie la création du voyage, du billet et des dépenses, puis leur suppression liée.
    void persistsTripTicketAndExpensesAndRemovesGeneratedTicketExpense() {
        // Crée un séjour et vérifie que la base lui a attribué un identifiant.
        var trip = travelDataService.createTrip(new TripRequest(
                "Séjour test", "France", "FR",
                LocalDate.of(2030, 4, 10), LocalDate.of(2030, 4, 20)));
        assertNotNull(trip.id());
        // Confirme qu’un seul voyage est enregistré dans ce test.
        assertEquals(1, tripRepository.count());

        // Ajoute un billet de train au voyage et vérifie la dépense de transport générée.
        var ticket = travelDataService.createTicket(trip.id(), new TicketRequest(
                "Train", "TGV test", "Paris", "Lyon",
                LocalDateTime.of(2030, 4, 10, 9, 0), null, 49.5, "TEST-1", null));
        assertNotNull(ticket.id());
        assertEquals(1, ticketRepository.count());
        assertEquals(1, expenseRepository.count());

        // Ajoute ensuite une dépense manuelle et contrôle le total du budget.
        travelDataService.createExpense(trip.id(), new ExpenseRequest(
                "Repas test", 12.5, LocalDate.of(2030, 4, 11),
                "Restauration", "Carte", null, false));
        assertEquals(2, expenseRepository.count());
        assertEquals(62.0, budgetRepository.findByTripId(trip.id()).orElseThrow().getActualTotal(), 0.001);

        // Supprime le billet et vérifie que seule la dépense manuelle subsiste.
        travelDataService.deleteTicket(ticket.id());
        assertEquals(0, ticketRepository.count());
        assertEquals(1, expenseRepository.count());
    }

    @Test
    // Vérifie la conservation des détails d’un vol et le calcul tenant compte des fuseaux horaires.
    void persistsFlightDetailsAndCalculatesElapsedDuration() {
        var trip = travelDataService.createTrip(new TripRequest(
                "Vol test", "France", "FR",
                LocalDate.of(2030, 4, 10), LocalDate.of(2030, 4, 20)));
        var details = new projet.saik.web.TravelDataDtos.FlightDetailsRequest(
                "Paris-Charles-de-Gaulle", "cdg", "New York-JFK", "jfk",
                1, "1 h 30 à Londres (LHR)", "Air Test", "AT123", "Economy Flex",
                "Bagage cabine", 8.0, "56 x 45 x 25 cm", "Oui", "40 x 30 x 15 cm",
                "1 pièce", 23.0, "158 cm",
                "Modifiable avec frais", "Remboursable", "Siège standard",
                "1 bagage supplémentaire", "Végétarien", "Embarquement prioritaire",
                "0571234567890", "Confirmé / OK", 60, -300);

        var ticket = travelDataService.createTicket(trip.id(), new TicketRequest(
                "Avion", "Vol AT123", "", "",
                LocalDateTime.of(2030, 4, 10, 10, 0),
                LocalDateTime.of(2030, 4, 10, 12, 30),
                420.0, "a1b2c3", null, details));

        assertEquals(510L, ticket.flightDetails().durationMinutes());
        assertEquals("CDG", ticket.flightDetails().departureIata());
        assertEquals("JFK", ticket.flightDetails().arrivalIata());
        assertEquals("0571234567890", ticket.flightDetails().eticketNumber());
        assertEquals("Paris-Charles-de-Gaulle (CDG)", ticket.departure());
        assertEquals("New York-JFK (JFK)", ticket.arrival());
        ticketRepository.flush();
        var storedDetails = ticketRepository.findById(ticket.id()).orElseThrow().getFlightDetails();
        assertEquals(510L, storedDetails.getDurationMinutes());
        assertEquals("56 x 45 x 25 cm", storedDetails.getCabinBaggageDimensions());
    }

    @Test
    // Vérifie qu’un changement d’adresse e-mail est conservé pendant toute la session de test.
    void profileUpdateRemainsAvailableAfterEmailChange() {
        // Remplace les informations du profil par une nouvelle adresse.
        var updated = travelDataService.updateProfile(new ProfileRequest(
                "Étudiant", "Démo", "", "Française",
                "nouveau@example.test", "", ""));

        // Vérifie la réponse de mise à jour et les lectures suivantes du profil.
        assertEquals("nouveau@example.test", updated.email());
        assertEquals("Étudiant", travelDataService.getProfile().firstName());
        assertEquals("nouveau@example.test", travelDataService.getProfile().email());
    }
}
