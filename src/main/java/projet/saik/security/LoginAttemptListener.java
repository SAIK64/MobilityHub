package projet.saik.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import projet.saik.repository.UserRepository;

@Component
// Alimente le compteur d’échecs à partir des événements d’authentification de Spring Security.
public class LoginAttemptListener {
    private final LoginAttemptService attempts;
    private final UserRepository users;

    public LoginAttemptListener(LoginAttemptService attempts, UserRepository users) {
        this.attempts = attempts;
        this.users = users;
    }

    @EventListener
    // Compte un échec pour l’adresse saisie (formulaire web comme API JWT).
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        attempts.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    // Efface le compteur après une connexion par mot de passe réussie.
    public void onSuccess(AuthenticationSuccessEvent event) {
        // Seules les connexions par identifiant et mot de passe concernent le compteur.
        if (!(event.getAuthentication() instanceof UsernamePasswordAuthenticationToken authentication)) {
            return;
        }
        // Le nom interne est l’identifiant numérique du compte : on retrouve l’adresse e-mail.
        try {
            users.findById(Long.valueOf(authentication.getName()))
                    .ifPresent(user -> attempts.reset(user.getEmail()));
        } catch (NumberFormatException ignored) {
            // Nom inattendu : rien à réinitialiser.
        }
    }
}
