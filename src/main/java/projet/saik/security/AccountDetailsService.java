package projet.saik.security;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import projet.saik.repository.UserRepository;

@Service
// Adapte les comptes de la base au format attendu par Spring Security.
public class AccountDetailsService implements UserDetailsService {
    // Permet de rechercher un compte à partir de son adresse e-mail.
    private final UserRepository users;
    // Sait si une adresse a dépassé le nombre d’essais de connexion autorisés.
    private final LoginAttemptService attempts;

    // Spring injecte le dépôt et le suivi des tentatives à utiliser pendant l’authentification.
    public AccountDetailsService(UserRepository users, LoginAttemptService attempts) {
        this.users = users;
        this.attempts = attempts;
    }

    @Override
    // Charge les informations Spring Security correspondant à l’adresse saisie.
    public UserDetails loadUserByUsername(String email) {
        // Normalise l’adresse puis renvoie une erreur générique si aucun compte ne correspond.
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        var user = users.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
        // Rassemble les rôles (ROLE_ADMIN…) et les permissions de ces rôles (USER_DELETE…).
        Set<String> authorities = new LinkedHashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(role.getName());
            role.getPermissions().forEach(permission -> authorities.add(permission.getName()));
        });
        // Utilise l’identifiant numérique comme nom interne pour lier proprement la session au compte.
        return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
                // Fournit à Spring l’empreinte BCrypt à comparer avec le mot de passe saisi.
                .password(user.getPassword())
                // Transmet rôles et permissions pour appliquer les règles d’accès.
                .authorities(authorities.toArray(String[]::new))
                // Un compte trop souvent mal authentifié est refusé avant même la vérification du mot de passe.
                .accountLocked(attempts.isBlocked(normalized))
                .build();
    }
}