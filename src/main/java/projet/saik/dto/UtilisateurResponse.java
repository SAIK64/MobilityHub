package projet.saik.dto;

import projet.saik.model.Role;

public record UtilisateurResponse(Long id, String email, Role role) {
}
