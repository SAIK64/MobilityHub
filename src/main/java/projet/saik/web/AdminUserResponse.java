package projet.saik.web;

import java.util.List;

// Vue d’un compte pour l’administration : jamais de mot de passe ni de données de profil sensibles.
public record AdminUserResponse(
        // Identifiant du compte.
        Long id,
        // Adresse e-mail de connexion.
        String email,
        // Prénom enregistré.
        String firstName,
        // Nom enregistré.
        String lastName,
        // Noms des rôles du compte, par exemple ROLE_USER.
        List<String> roles,
        // Nombre de voyages du compte.
        int tripCount) {
}
