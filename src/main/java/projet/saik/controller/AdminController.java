package projet.saik.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import projet.saik.service.AdminService;
import projet.saik.web.AdminUserResponse;

@RestController
@RequestMapping("/api/admin")
// Routes d’administration : protégées par l’URL (SecurityConfig) et par la méthode (@PreAuthorize).
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    // Liste les comptes enregistrés.
    public List<AdminUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    // Supprime un compte ; exige la permission USER_DELETE.
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, Authentication authentication) {
        // Le nom de l’authentification est l’identifiant du compte, en session comme en JWT.
        adminService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
