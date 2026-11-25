package projet.saik.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// contient les données reçues pour l’inscription ou l’authentification, avec des règles de validation comme le format de l’e-mail et la longueur du mot de passe.
public class AuthRequest {
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
    private String lastName;

    // Rejette une adresse vide ou qui n’a pas un format d’e-mail valide.
    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "Adresse e-mail invalide.")
    private String email;

    // Refuse un mot de passe vide ou situé hors des limites acceptées.
    @NotBlank(message = "Le mot de passe est obligatoire.")
    @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
    private String password;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // Retourne l’adresse saisie pour la validation et l’authentification.
    public String getEmail() { return email; }
    // Reçoit l’adresse fournie par le formulaire ou le JSON.
    public void setEmail(String email) { this.email = email; }
    // Retourne le mot de passe reçu avant son encodage par le service.
    public String getPassword() { return password; }
    // Reçoit le mot de passe transmis pour l’inscription ou la connexion.
    public void setPassword(String password) { this.password = password; }
}
