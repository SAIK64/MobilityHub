package projet.saik.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
//évite d'écrire manuellement tous les getters et setters
@Data
public class UserRegisterDTO {
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;
    
    @NotBlank(message = "Le nom de famille est obligatoire")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    private String email;
    
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    // Constructeur par défaut requis pour la désérialisation JSON.
    public UserRegisterDTO() {}
    // Constructeur pratique pour créer un DTO à partir des champs essentiels.
    public UserRegisterDTO(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }
}
