package projet.saik.security;

import java.util.Locale;

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

    // Spring injecte le dépôt à utiliser pendant l’authentification.
    public AccountDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    // Charge les informations Spring Security correspondant à l’adresse saisie.
    public UserDetails loadUserByUsername(String email) {
        // Normalise l’adresse puis renvoie une erreur générique si aucun compte ne correspond.
        var user = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
        // Utilise l’identifiant numérique comme nom interne pour lier proprement la session au compte.
        return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
                // Fournit à Spring l’empreinte BCrypt à comparer avec le mot de passe saisi.
                .password(user.getPassword())
                // Transmet les rôles du compte pour appliquer les règles d’accès.
                .authorities(user.getRoles().stream().map(role -> role.getName()).toArray(String[]::new))
                .build();
    }
}
