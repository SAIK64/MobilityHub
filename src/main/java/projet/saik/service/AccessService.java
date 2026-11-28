package projet.saik.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import projet.saik.model.Budget;
import projet.saik.model.Trip;
import projet.saik.model.User;
import projet.saik.repository.BudgetRepository;
import projet.saik.repository.TripRepository;
import projet.saik.repository.UserRepository;

@Service
// Contrôles d’accès partagés par les services : utilisateur connecté, propriété d’un voyage, budget d’un voyage.
public class AccessService {

    // Accès aux comptes afin de retrouver la personne connectée.
    private final UserRepository userRepository;
    // Accès aux voyages pour vérifier leur propriétaire.
    private final TripRepository tripRepository;
    // Accès aux budgets associés aux voyages.
    private final BudgetRepository budgetRepository;

    public AccessService(UserRepository userRepository, TripRepository tripRepository, BudgetRepository budgetRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.budgetRepository = budgetRepository;
    }

    // Résout l’utilisateur de la session, qu’il soit authentifié par JWT ou par session classique.
    public User requireCurrentUser() {
        // Lit l’authentification fournie par le contexte de sécurité Spring.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Interrompt la requête si aucun utilisateur authentifié n’est présent.
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
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
    public Trip requireOwnedTrip(Long tripId) {
        // Détermine le propriétaire à partir de la session vérifiée.
        User user = requireCurrentUser();
        // Recherche simultanément par identifiant de voyage et identifiant de propriétaire.
        return tripRepository.findByIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voyage introuvable."));
    }

    // Renvoie le budget du voyage ou en crée un pour les anciennes données qui n’en auraient pas.
    public Budget requireBudget(Trip trip) {
        return budgetRepository.findByTripId(trip.getId()).orElseGet(() -> {
            // Construit un budget associé au voyage.
            Budget budget = new Budget();
            budget.setTrip(trip);
            trip.setBudget(budget);
            // Enregistre le budget avant de le retourner.
            return budgetRepository.save(budget);
        });
    }
}
