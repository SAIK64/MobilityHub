package projet.saik.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projet.saik.model.User;
import projet.saik.service.AccountService;
import projet.saik.dto.UserRegisterDTO;
import projet.saik.dto.UserResponseDTO;
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserRestController {
    private final AccountService accountService;

    public UserRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRegisterDTO request) {
        User createdUser = accountService.register(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        UserResponseDTO response = toResponseDto(createdUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UserResponseDTO toResponseDto(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }
}
