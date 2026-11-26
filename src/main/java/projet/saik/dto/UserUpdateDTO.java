package projet.saik.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateDTO {
    private String firstName;
    private String lastName;

    @Email(message = "Format d'email invalide")
    private String email;

    private String password;

    public UserUpdateDTO() {}

    public UserUpdateDTO(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }
}