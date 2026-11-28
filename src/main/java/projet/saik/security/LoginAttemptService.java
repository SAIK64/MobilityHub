package projet.saik.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
// Limite les essais de connexion : après trop d’échecs, le compte est bloqué un temps donné.
public class LoginAttemptService {
    // Au-delà de cette taille, les entrées expirées sont purgées pour borner la mémoire.
    private static final int MAX_TRACKED_KEYS = 10_000;

    // Nombre d’échecs tolérés avant blocage.
    private final int maxAttempts;
    // Durée du blocage, qui sert aussi de fenêtre de comptage des échecs.
    private final Duration lockDuration;
    // Échecs en cours par adresse e-mail normalisée.
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${saik.security.max-login-attempts:5}") int maxAttempts,
            @Value("${saik.security.lockout-minutes:15}") long lockoutMinutes) {
        if (maxAttempts <= 0 || lockoutMinutes <= 0) {
            throw new IllegalArgumentException("Les limites de connexion doivent être strictement positives.");
        }
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockoutMinutes);
    }

    // Enregistre un échec ; au seuil, le blocage démarre pour toute la durée configurée.
    public void recordFailure(String email) {
        if (attempts.size() > MAX_TRACKED_KEYS) {
            purgeExpired();
        }
        Instant now = Instant.now();
        attempts.compute(normalize(email), (key, current) -> {
            // Repart de zéro si la série précédente est trop ancienne.
            if (current == null || current.isExpired(now)) {
                current = new Attempt(0, now.plus(lockDuration));
            }
            int count = current.count() + 1;
            return new Attempt(count, now.plus(lockDuration));
        });
    }

    // Indique si l’adresse a atteint le seuil d’échecs récemment.
    public boolean isBlocked(String email) {
        Attempt current = attempts.get(normalize(email));
        return current != null && !current.isExpired(Instant.now()) && current.count() >= maxAttempts;
    }

    // Efface le compteur après une connexion réussie.
    public void reset(String email) {
        attempts.remove(normalize(email));
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    // Série d’échecs et date à laquelle elle cesse de compter.
    private record Attempt(int count, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }
}
