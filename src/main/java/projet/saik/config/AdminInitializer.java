package projet.saik.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionTemplate;

import projet.saik.repository.RoleRepository;
import projet.saik.repository.UserRepository;
import projet.saik.service.AccountService;
import projet.saik.service.AdminService;

@Configuration
// Crée ou promeut le premier administrateur à partir de variables d’environnement.
public class AdminInitializer {
    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    @Bean
    @Order(2)
    public CommandLineRunner initAdmin(
            @Value("${saik.admin.email:}") String email,
            @Value("${saik.admin.password:}") String password,
            AccountService accounts,
            UserRepository users,
            RoleRepository roles,
            TransactionTemplate transactions) {
        return args -> {
            if (email.isBlank()) {
                return;
            }
            transactions.executeWithoutResult(status -> {
                var adminRole = roles.findByName(AdminService.ADMIN_ROLE)
                        .orElseThrow(() -> new IllegalStateException("Le rôle administrateur est absent."));
                var existing = users.findByEmailIgnoreCase(email.trim());
                var admin = existing.orElseGet(() -> accounts.register(email, password, "Administrateur", "SAIK"));
                if (admin.getRoles().stream().noneMatch(role -> AdminService.ADMIN_ROLE.equals(role.getName()))) {
                    admin.getRoles().add(adminRole);
                    users.save(admin);
                    log.info("Le compte {} est administrateur.", admin.getEmail());
                }
            });
        };
    }
}
