package projet.saik.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import projet.saik.model.User;
import projet.saik.repository.RoleRepository;
import projet.saik.repository.UserRepository;

@Service
// Regroupe les règles métier liées à la création des comptes.
public class AccountService {
    // Dépôt qui enregistre les comptes et recherche les e-mails existants.
    private final UserRepository users;
    // Dépôt qui fournit le rôle attribué par défaut.
    private final RoleRepository roles;
    // Encodeur BCrypt fourni par la configuration de sécurité.
    private final PasswordEncoder passwords;

    // Reçoit les dépendances nécessaires pour créer un compte complet.
    public AccountService(UserRepository users, RoleRepository roles, PasswordEncoder passwords) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
    }

    @Transactional
    // Valide les informations, encode le mot de passe et enregistre le nouveau compte.
    public User register(String email, String password, String firstName, String lastName) {
        // Uniformise l’adresse afin de prévenir les doublons dus aux espaces ou à la casse.
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        // Refuse une adresse déjà associée à un compte.
        if (users.findByEmailIgnoreCase(normalized).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
        }
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();
        if (normalizedFirstName.isBlank() || normalizedLastName.isBlank()
                || normalizedFirstName.length() > 100 || normalizedLastName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le prénom et le nom sont obligatoires et ne peuvent pas dépasser 100 caractères.");
        }
        // Contrôle la longueur minimale et la limite en octets imposée par BCrypt.
        if (password.isBlank() || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins 8 caractères et au maximum 72 octets.");
        }
        // Prépare le compte et renseigne les champs initiaux du profil.
        User user = new User();
        user.setFirstName(normalizedFirstName);
        user.setLastName(normalizedLastName);
        user.setEmail(normalized);
        // Enregistre uniquement l’empreinte du mot de passe, jamais le texte en clair.
        user.setPassword(passwords.encode(password));
        // Attribue le rôle standard et signale une configuration incomplète s’il est absent.
        user.setRoles(Set.of(roles.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Le rôle utilisateur est absent."))));
        // Persiste le compte et force l’écriture pour obtenir les erreurs de base immédiatement.
        return users.saveAndFlush(user);
    }
}
