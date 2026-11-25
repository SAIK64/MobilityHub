package projet.saik.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import projet.saik.model.Country;
import projet.saik.model.Permission;
import projet.saik.model.Role;
import projet.saik.repository.CountryRepository;
import projet.saik.repository.PermissionRepository;
import projet.saik.repository.RoleRepository;

@Configuration
// Prépare les rôles, permissions et pays de base au premier démarrage.
public class DataInitializer {

    @Bean
    @Order(1)
    // Exécute ces données de référence avant les autres initialiseurs.
    CommandLineRunner initDatabase(
            PermissionRepository permissionRepo,
            RoleRepository roleRepo,
            CountryRepository countryRepo) {

        return args -> {
            // Crée les rôles standards uniquement si aucun rôle n’existe encore.
            if (roleRepo.count() == 0) {
                // Enregistre les permissions utilisées par les contrôles d’accès.
                Permission tripRead = permissionRepo.save(new Permission("TRIP_READ"));
                Permission tripWrite = permissionRepo.save(new Permission("TRIP_WRITE"));
                Permission userDelete = permissionRepo.save(new Permission("USER_DELETE"));

                // Le rôle utilisateur peut consulter et modifier ses voyages.
                Role userRole = new Role("ROLE_USER");
                userRole.setPermissions(Set.of(tripRead, tripWrite));
                roleRepo.save(userRole);

                // Le rôle administrateur reçoit aussi le droit de supprimer des comptes.
                Role adminRole = new Role("ROLE_ADMIN");
                adminRole.setPermissions(Set.of(tripRead, tripWrite, userDelete));
                roleRepo.save(adminRole);
            }
            // Répare une base existante dans laquelle le rôle utilisateur serait absent.
            if (roleRepo.findByName("ROLE_USER").isEmpty()) {
                roleRepo.save(new Role("ROLE_USER"));
            }

            // Ajoute un petit catalogue de pays uniquement si la table est vide.
            if (countryRepo.count() == 0) {
                countryRepo.save(new Country("France", "FR"));
                countryRepo.save(new Country("Japon", "JP"));
                countryRepo.save(new Country("Espagne", "ES"));
            }
        };
    }
}