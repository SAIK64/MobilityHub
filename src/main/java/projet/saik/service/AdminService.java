package projet.saik.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import projet.saik.model.Role;
import projet.saik.model.User;
import projet.saik.repository.UserRepository;
import projet.saik.web.AdminUserResponse;

@Service
// Règles métier des opérations réservées aux administrateurs.
public class AdminService {
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UserRepository users;

    public AdminService(UserRepository users) {
        this.users = users;
    }

    // Indique si le compte possède le rôle administrateur.
    public boolean isAdmin(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(ADMIN_ROLE::equals);
    }

    @Transactional(readOnly = true)
    // Liste tous les comptes, sans aucune donnée sensible.
    public List<AdminUserResponse> listUsers() {
        return users.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    // Supprime un compte et, en cascade, ses voyages ; un administrateur ne peut pas supprimer son propre compte.
    public void deleteUser(Long userId, String currentUserId) {
        if (String.valueOf(userId).equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vous ne pouvez pas supprimer votre propre compte.");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable."));
        users.delete(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getTrips().size());
    }
}
