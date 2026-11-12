package projet.saik.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projet.saik.model.Role;
import projet.saik.model.User;
import projet.saik.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User inscrire(String email, String motDePasseClair) {
        return creer(email, motDePasseClair, Role.ETUDIANT);
    }

    @Transactional
    public Utilisateur creer(String email, String motDePasseClair, Role role) {
        String emailNormalise = email.trim().toLowerCase();
        if (utilisateurRepository.existsByEmail(emailNormalise)) {
            throw new EmailDejaUtiliseException(emailNormalise);
        }
        return userRepository.save(
                new Utilisateur(emailNormalise, passwordEncoder.encode(motDePasseClair), role));
    }
}
