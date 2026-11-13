package projet.saik.config;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import projet.saik.repository.UserRepository;

@Configuration
// Convertit les anciens mots de passe enregistrés en clair vers des empreintes BCrypt.
public class LegacyPasswordMigration {
    @Bean
    // Exécute la migration des comptes au démarrage, dans une transaction.
    public CommandLineRunner migrateLegacyPasswords(UserRepository users, PasswordEncoder encoder,
            TransactionTemplate transactions, @Value("${saik.legacy-demo-password:}") String demoPassword) {
        return args -> transactions.executeWithoutResult(status -> users.findAll().forEach(user -> {
            // Ne réencode pas un mot de passe BCrypt déjà valide.
            String password = user.getPassword();
            if (password.matches("^\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}$")) {
                return;
            }
            // Une empreinte commençant par BCrypt mais mal formée indique une base à corriger.
            if (password.startsWith("$2")) {
                throw new IllegalStateException("Empreinte BCrypt invalide : vérifiez la base de données.");
            }
            // Remplace l’ancien mot de passe de démonstration seulement si un secret neuf est fourni.
            if (user.getEmail().equalsIgnoreCase("admin@saik.fr") && password.equals("admin123")) {
                if (demoPassword.length() < 8 || demoPassword.getBytes(StandardCharsets.UTF_8).length > 72
                        || demoPassword.equals("admin123")) {
                    throw new IllegalStateException("Définissez SAIK_LEGACY_DEMO_PASSWORD avec un nouveau mot de passe"
                            + " d'au moins 8 caractères et de 72 octets maximum pour récupérer le compte de démonstration.");
                }
                password = demoPassword;
            }
            // BCrypt ne peut traiter que des mots de passe allant jusqu’à 72 octets.
            if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Mot de passe historique trop long pour BCrypt : réinitialisation nécessaire.");
            }
            // Encode puis sauvegarde le mot de passe historique.
            user.setPassword(encoder.encode(password));
            users.save(user);
        }));
    }
}
