package projet.saik.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import projet.saik.model.Budget;
import projet.saik.model.Country;
import projet.saik.model.Expense;
import projet.saik.model.TransportTicket;
import projet.saik.model.Trip;
import projet.saik.model.User;
import projet.saik.repository.BudgetRepository;
import projet.saik.repository.CountryRepository;
import projet.saik.repository.ExpenseRepository;
import projet.saik.repository.TransportTicketRepository;
import projet.saik.repository.TripRepository;
import projet.saik.repository.UserRepository;
import projet.saik.web.TravelDataDtos.ExpenseRequest;
import projet.saik.web.TravelDataDtos.ExpenseResponse;
import projet.saik.web.TravelDataDtos.ProfileRequest;
import projet.saik.web.TravelDataDtos.ProfileResponse;
import projet.saik.web.TravelDataDtos.TicketRequest;
import projet.saik.web.TravelDataDtos.TicketResponse;
import projet.saik.web.TravelDataDtos.TripRequest;
import projet.saik.web.TravelDataDtos.TripResponse;

@Service
// Contient les règles métier partagées par les routes de l’API de voyage.
public class TravelDataService {

    // Accès aux voyages enregistrés.
    private final TripRepository tripRepository;
    // Accès au catalogue des pays.
    private final CountryRepository countryRepository;
    // Accès aux comptes afin de retrouver la personne connectée.
    private final UserRepository userRepository;
    // Accès aux budgets associés aux voyages.
    private final BudgetRepository budgetRepository;
    // Accès aux dépenses manuelles ou générées automatiquement.
    private final ExpenseRepository expenseRepository;
    // Accès aux titres de transport enregistrés.
    private final TransportTicketRepository ticketRepository;

    // Spring fournit les dépôts nécessaires à chaque règle métier.
    public TravelDataService( TripRepository tripRepository, CountryRepository countryRepository, UserRepository userRepository, BudgetRepository budgetRepository, ExpenseRepository expenseRepository, TransportTicketRepository ticketRepository) {
        // Conserve le dépôt des voyages pour les opérations de lecture et de création.
        this.tripRepository = tripRepository;
        // Conserve le dépôt des pays pour réutiliser les pays déjà connus.
        this.countryRepository = countryRepository;
        // Conserve le dépôt des comptes pour contrôler l’identité et le profil.
        this.userRepository = userRepository;
        // Conserve le dépôt des budgets pour rattacher les dépenses.
        this.budgetRepository = budgetRepository;
        // Conserve le dépôt des dépenses pour les enregistrer et les supprimer.
        this.expenseRepository = expenseRepository;
        // Conserve le dépôt des titres pour les enregistrer et les supprimer.
        this.ticketRepository = ticketRepository;
    }

    // La lecture seule évite de modifier accidentellement les données en listant les voyages.
    @Transactional(readOnly = true)
    public List<TripResponse> getTrips() {
        // Récupère l’identité vérifiée de la personne connectée.
        User user = requireCurrentUser();
        // Ne charge que ses voyages, les trie par date de départ, puis les convertit en réponses.
        return tripRepository.findByUserIdOrderByStartDateAsc(user.getId())
                .stream()
                .map(this::toTripResponse)
                .toList();
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {
        // Refuse une période incohérente avant toute écriture en base.
        if (request.end().isBefore(request.start())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit suivre la date de départ.");
        }

        // Attache le nouveau voyage au compte authentifié.
        User user = requireCurrentUser();
        // Uniformise le code pays afin de retrouver plus facilement le pays en base.
        String iso = request.iso().toUpperCase();
        // Réutilise le pays existant ou l’ajoute au catalogue s’il n’y est pas encore.
        Country country = countryRepository.findByCodeIso(iso).orElseGet(() -> {
            // Construit le pays avec un nom débarrassé des espaces superflus.
            Country created = new Country(request.country().trim(), iso);
            // Persiste le nouveau pays et le retourne au traitement courant.
            return countryRepository.save(created);
        });

        // Prépare une nouvelle entité de voyage.
        Trip trip = new Trip();
        // Nettoie le nom fourni avant de l’enregistrer.
        trip.setName(request.name().trim());
        // Enregistre le premier jour du séjour.
        trip.setStartDate(request.start());
        // Enregistre le dernier jour du séjour.
        trip.setEndDate(request.end());
        // Lie le voyage à son propriétaire.
        trip.setUser(user);
        // Associe au voyage le pays choisi.
        trip.getCountries().add(country);

        // Chaque voyage possède un budget créé au même moment.
        Budget budget = new Budget();
        // Relie le budget au voyage.
        budget.setTrip(trip);
        // Maintient aussi la relation dans l’objet voyage.
        trip.setBudget(budget);

        // Enregistre le voyage, puis renvoie sa représentation destinée à l’API.
        return toTripResponse(tripRepository.save(trip));
    }

    // Consulte les titres après vérification de la propriété du voyage.
    @Transactional(readOnly = true)
    public List<TicketResponse> getTickets(Long tripId) {
        // Refuse l’accès si le voyage n’existe pas ou appartient à quelqu’un d’autre.
        Trip trip = requireOwnedTrip(tripId);
        // Trie les titres par départ et convertit chaque entité en réponse JSON.
        return ticketRepository.findByTripIdOrderByDepartureTimeAsc(trip.getId())
                .stream()
                .map(this::toTicketResponse)
                .toList();
    }

    @Transactional
    public TicketResponse createTicket(Long tripId, TicketRequest request) {
        // Vérifie l’accès au voyage avant de lui ajouter des données.
        Trip trip = requireOwnedTrip(tripId);
        // Récupère le budget existant ou le crée si nécessaire.
        Budget budget = requireBudget(trip);

        // Prépare l’entité représentant le titre de transport.
        TransportTicket ticket = new TransportTicket();
        // Enregistre le type de transport sans espaces inutiles.
        ticket.setTransportType(request.type().trim());
        // Enregistre la description saisie par la personne.
        ticket.setManualDescription(request.description().trim());
        // Enregistre les lieux si l’interface les a fournis.
        ticket.setDeparture(request.departure());
        ticket.setArrival(request.arrival());
        // Enregistre les horaires du trajet.
        ticket.setDepartureTime(request.departureTime());
        ticket.setArrivalTime(request.arrivalTime());
        // Enregistre le prix, la référence et le nom du justificatif.
        ticket.setPrice(request.price());
        ticket.setApiBookingReference(request.reference());
        ticket.setAttachmentName(request.attachmentName());
        // Relie ce titre au voyage concerné.
        ticket.setTrip(trip);

        // Un prix strictement positif crée aussi une dépense liée au transport.
        if (request.price() > 0) {
            // Prépare la dépense qui apparaîtra dans le budget du voyage.
            Expense generatedExpense = new Expense();
            // Reprend la description du titre comme libellé de la dépense.
            generatedExpense.setDescription(request.description().trim());
            // Copie le montant du titre dans le budget.
            generatedExpense.setAmount(request.price());
            // Classe cette dépense dans la catégorie transport.
            generatedExpense.setCategory("Transport");
            // Utilise la date de départ comme date de dépense.
            generatedExpense.setExpenseDate(request.departureTime().toLocalDate());
            // Marque la dépense pour pouvoir la distinguer des dépenses saisies manuellement.
            generatedExpense.setIsAutoGenerated(true);
            // Rattache la dépense au budget du voyage.
            generatedExpense.setBudget(budget);
            // Met à jour la collection en mémoire avant l’enregistrement.
            budget.getExpenses().add(generatedExpense);
            // Sauvegarde la dépense puis la relie au titre de transport.
            ticket.setGeneratedExpense(expenseRepository.save(generatedExpense));
        }

        // Enregistre le titre et renvoie ses données à l’interface.
        return toTicketResponse(ticketRepository.save(ticket));
    }

    // Supprime un titre et la dépense automatique qu’il aurait créée.
    @Transactional
    public void deleteTicket(Long ticketId) {
        // Recherche le titre ou répond avec une erreur 404 s’il est absent.
        TransportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Titre introuvable."));
        // Contrôle que le titre appartient bien à un voyage de la personne connectée.
        requireOwnedTrip(ticket.getTrip().getId());
        // Garde une référence à la dépense avant de supprimer le titre.
        Expense generatedExpense = ticket.getGeneratedExpense();
        // Supprime le titre de la base.
        ticketRepository.delete(ticket);
        // Si le titre avait généré une dépense, retire-la aussi du budget et de la base.
        if (generatedExpense != null) {
            generatedExpense.getBudget().getExpenses().remove(generatedExpense);
            expenseRepository.delete(generatedExpense);
        }
    }

    @Transactional(readOnly = true)
    // Liste les dépenses du voyage après contrôle des droits d’accès.
    public List<ExpenseResponse> getExpenses(Long tripId) {
        // Vérifie que le voyage appartient à la personne connectée.
        Trip trip = requireOwnedTrip(tripId);
        // Charge son budget, trie les dépenses par date décroissante et prépare les réponses.
        return budgetRepository.findByTripId(trip.getId())
                .map(budget -> expenseRepository.findByBudgetIdOrderByExpenseDateDesc(budget.getId())
                        .stream()
                        .map(this::toExpenseResponse)
                        .toList())
                .orElseGet(List::of);
    }

    @Transactional
    // Enregistre une dépense saisie dans l’interface.
    public ExpenseResponse createExpense(Long tripId, ExpenseRequest request) {
        // Vérifie la propriété du voyage avant de modifier son budget.
        Trip trip = requireOwnedTrip(tripId);
        // Récupère ou initialise le budget associé.
        Budget budget = requireBudget(trip);

        // Prépare l’entité de dépense.
        Expense expense = new Expense();
        // Nettoie le libellé et la catégorie, puis copie les informations fournies.
        expense.setDescription(request.description().trim());
        expense.setAmount(request.amount());
        expense.setCategory(request.category().trim());
        expense.setExpenseDate(request.date());
        expense.setPaymentMethod(request.paymentMethod());
        expense.setReceiptName(request.receiptName());
        // Seule la valeur booléenne true active le statut automatique.
        expense.setIsAutoGenerated(Boolean.TRUE.equals(request.autoGenerated()));
        // Rattache la dépense au budget et met à jour la relation en mémoire.
        expense.setBudget(budget);
        budget.getExpenses().add(expense);

        // Persiste la dépense puis renvoie les données utiles à l’interface.
        return toExpenseResponse(expenseRepository.save(expense));
    }

    // Supprime une dépense et détache proprement un éventuel titre associé.
    @Transactional
    public void deleteExpense(Long expenseId) {
        // Recherche la dépense ou retourne une erreur 404 si elle n’existe pas.
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dépense introuvable."));
        // Vérifie que son voyage appartient à la personne authentifiée.
        requireOwnedTrip(expense.getBudget().getTrip().getId());
        // Repère le titre si cette dépense a été générée à partir d’un transport.
        TransportTicket ticket = expense.getTransportTicket();
        // Retire le lien inverse pour éviter de conserver une référence vers la dépense supprimée.
        if (ticket != null) {
            ticket.setGeneratedExpense(null);
            ticketRepository.save(ticket);
        }
        // Retire la dépense de la collection du budget avant sa suppression en base.
        expense.getBudget().getExpenses().remove(expense);
        expenseRepository.delete(expense);
    }

    // Retourne le profil du compte actuellement connecté.
    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        // Réutilise le contrôle d’identité et convertit le compte en réponse API.
        return toProfileResponse(requireCurrentUser());
    }

    // Met à jour les informations modifiables du profil de la personne connectée.
    @Transactional
    public ProfileResponse updateProfile(ProfileRequest request) {
        // Charge le compte à modifier à partir de la session authentifiée.
        User user = requireCurrentUser();
        // Refuse une adresse déjà utilisée par un autre compte, sans bloquer le compte courant.
        userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
                });

        // Copie les nouvelles coordonnées en retirant les espaces superflus des champs texte.
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        // Normalise l’adresse en minuscules pour éviter les doublons dus à la casse.
        user.setEmail(request.email().trim().toLowerCase(java.util.Locale.ROOT));
        // Met à jour les autres informations facultatives du profil.
        user.setBirthDate(request.birthDate());
        user.setNationality(request.nationality());
        user.setPhone(request.phone());
        user.setPassport(request.passport());
        // Sauvegarde le compte et renvoie son profil mis à jour.
        return toProfileResponse(userRepository.save(user));
    }

    // Résout l’utilisateur de la session, qu’il soit authentifié par JWT ou par session classique.
    private User requireCurrentUser() {
        // Lit l’authentification fournie par le contexte de sécurité Spring.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Interrompt la requête si aucun utilisateur authentifié n’est présent.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        }
        // Le JWT contient l’identifiant dans son sujet ; sinon, le nom de session sert d’identifiant.
        String id = authentication instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getSubject() : authentication.getName();
        // Prépare l’identifiant numérique attendu par le dépôt.
        Long userId;
        try {
            // Convertit l’identifiant textuel de la session en identifiant de base de données.
            userId = Long.valueOf(id);
        } catch (NumberFormatException ex) {
            // Signale une session invalide si son identifiant ne peut pas être converti.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide.", ex);
        }
        // Charge le compte ou renvoie une erreur d’authentification s’il n’existe plus.
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Compte introuvable."));
    }

    // Charge un voyage uniquement s’il appartient à la personne connectée.
    private Trip requireOwnedTrip(Long tripId) {
        // Détermine le propriétaire à partir de la session vérifiée.
        User user = requireCurrentUser();
        // Recherche simultanément par identifiant de voyage et identifiant de propriétaire.
        return tripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable."));
    }

    // Renvoie le budget du voyage ou en crée un pour les anciennes données qui n’en auraient pas.
    private Budget requireBudget(Trip trip) {
        return budgetRepository.findByTripId(trip.getId()).orElseGet(() -> {
            // Construit un budget associé au voyage.
            Budget budget = new Budget();
            budget.setTrip(trip);
            trip.setBudget(budget);
            // Enregistre le budget avant de le retourner.
            return budgetRepository.save(budget);
        });
    }

    // Convertit l’entité de voyage en réponse JSON sans exposer l’entité JPA directement.
    private TripResponse toTripResponse(Trip trip) {
        // Utilise le premier pays associé, ou une valeur vide si aucun pays n’est disponible.
        Country country = trip.getCountries().stream().findFirst().orElse(null);
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                country == null ? "" : country.getName(),
                country == null ? "" : country.getCodeIso(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCountries().size()
            );
    }

    // Convertit un titre JPA vers le format stable attendu par l’interface.
    private TicketResponse toTicketResponse(TransportTicket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTransportType(),
                ticket.getManualDescription(),
                ticket.getDeparture(),
                ticket.getArrival(),
                ticket.getDepartureTime(),
                ticket.getArrivalTime(),
                ticket.getPrice(),
                ticket.getApiBookingReference(),
                ticket.getAttachmentName());
    }

    // Convertit une dépense JPA en réponse destinée à l’interface.
    private ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getPaymentMethod(),
                expense.getReceiptName(),
                expense.getIsAutoGenerated());
    }

    // Convertit le compte utilisateur en données de profil.
    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                user.getNationality(),
                user.getEmail(),
                user.getPhone(),
                user.getPassport());
    }
}
