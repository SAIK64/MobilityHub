package projet.saik.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Modèle des champs reçus depuis les formulaires d’inscription ou les requêtes d’authentification.
public class AuthRequest {
    // Rejette une adresse vide ou qui n’a pas un format d’e-mail valide.
    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "Adresse e-mail invalide.")
    private String email;

    // Refuse un mot de passe vide ou situé hors des limites acceptées.
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
    private String password;

    // Retourne l’adresse saisie pour la validation et l’authentification.
    public String getEmail() { return email; }
    // Reçoit l’adresse fournie par le formulaire ou le JSON.
    public void setEmail(String email) { this.email = email; }
    // Retourne le mot de passe reçu avant son encodage par le service.
    public String getPassword() { return password; }
    // Reçoit le mot de passe transmis pour l’inscription ou la connexion.
    public void setPassword(String password) { this.password = password; }
}
