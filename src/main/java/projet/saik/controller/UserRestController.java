package projet.saik.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import projet.saik.model.User;
import projet.saik.service.AccountService;
import projet.saik.dto.UserRegisterDTO;
import projet.saik.dto.UserResponseDTO;

@RestController
// Le contrôleur REST pour la gestion des utilisateurs, accessible via ce choix de URL /api/v1/admin/users pour eviter les conflits avec d’autres contrôleurs sur le navigateur
@RequestMapping("/api/v1/admin/users")
public class UserRestController {
    //final pour eviter la modification de la référence de l’objet AccountService après l’initialisation.
    private final AccountService accountService;

    public UserRestController(AccountService accountService) {
        this.accountService = accountService;
    }
    // Crée un compte utilisateur via l’API REST ; exige le rôle ADMIN.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    //@Valid pour declencher la verfication des contraintes de validation sur le DTO UserRegisterDTO.
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRegisterDTO request) {
        User createdUser = accountService.register(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );
        // Transforme l’entité User en DTO pour la réponse.
        UserResponseDTO response = toResponseDto(createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    // Transforme l’entité User en DTO pour avoir la confirmation de la création du compte.

    private UserResponseDTO toResponseDto(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
