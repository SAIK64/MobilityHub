package projet.saik.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import projet.saik.model.User;
import projet.saik.repository.UserRepository;
import projet.saik.security.LoginAttemptService;
import projet.saik.web.TravelDataDtos.ProfileRequest;
import projet.saik.web.TravelDataDtos.ProfileResponse;

@Service
// Règles métier du profil de la personne connectée.
public class ProfileService {

    // Accès aux comptes pour enregistrer le profil.
    private final UserRepository userRepository;
    // Vérifie le mot de passe actuel avant les changements sensibles.
    private final PasswordEncoder passwordEncoder;
    // Limite les essais de mot de passe, comme à la connexion.
    private final LoginAttemptService loginAttempts;
    // Contrôles d’identité.
    private final AccessService access;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder, LoginAttemptService loginAttempts, AccessService access) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.access = access;
    }

    // Retourne le profil du compte actuellement connecté.
    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        // Réutilise le contrôle d’identité et convertit le compte en réponse API.
        return toProfileResponse(access.requireCurrentUser());
    }

    // Met à jour les informations modifiables du profil de la personne connectée.
    @Transactional
    public ProfileResponse updateProfile(ProfileRequest request) {
        // Charge le compte à modifier à partir de la session authentifiée.
        User user = access.requireCurrentUser();
        String newEmail = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        // Changer d’adresse change l’identifiant de connexion : le mot de passe actuel est exigé.
        if (!newEmail.equalsIgnoreCase(user.getEmail())) {
            requireCurrentPassword(user, request.currentPassword());
        }
        // Refuse une adresse déjà utilisée par un autre compte, sans bloquer le compte courant.
        userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse e-mail est déjà utilisée.");
                });

        // Copie les nouvelles coordonnées en retirant les espaces superflus des champs texte.
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        // Normalise l’adresse en minuscules pour éviter les doublons dus à la casse.
        user.setEmail(newEmail);
        // Met à jour les autres informations facultatives du profil.
        user.setBirthDate(request.birthDate());
        user.setNationality(request.nationality());
        user.setPhone(request.phone());
        user.setPassport(request.passport());
        // Sauvegarde le compte et renvoie son profil mis à jour.
        return toProfileResponse(userRepository.save(user));
    }

    // Refuse l’opération si le mot de passe actuel est absent ou faux, avec la même limite d’essais qu’à la connexion.
    private void requireCurrentPassword(User user, String currentPassword) {
        if (loginAttempts.isBlocked(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives. Réessayez plus tard.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe actuel est requis pour changer l'adresse e-mail.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            loginAttempts.recordFailure(user.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mot de passe actuel incorrect.");
        }
        loginAttempts.reset(user.getEmail());
    }

    // Convertit le compte utilisateur en données de profil.
    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                user.getNationality(),
                user.getEmail(),
                user.getPhone(),
                user.getPassport());
    }
}
