package projet.saik.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import projet.saik.model.Country;
import projet.saik.model.Permission;
import projet.saik.model.Role;
import projet.saik.model.User;
import projet.saik.repository.CountryRepository;
import projet.saik.repository.PermissionRepository;
import projet.saik.repository.RoleRepository;
import projet.saik.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            PermissionRepository permissionRepo,
            RoleRepository roleRepo,
            UserRepository userRepo,
            CountryRepository countryRepo) {

        return args -> {
            if (roleRepo.count() == 0) {
                Permission tripRead = permissionRepo.save(new Permission("TRIP_READ"));
                Permission tripWrite = permissionRepo.save(new Permission("TRIP_WRITE"));
                Permission userDelete = permissionRepo.save(new Permission("USER_DELETE"));

                Role userRole = new Role("ROLE_USER");
                userRole.setPermissions(Set.of(tripRead, tripWrite));
                roleRepo.save(userRole);

                Role adminRole = new Role("ROLE_ADMIN");
                adminRole.setPermissions(Set.of(tripRead, tripWrite, userDelete));
                roleRepo.save(adminRole);

                if (userRepo.count() == 0) {
                    User admin = new User();
                    admin.setFirstName("Admin");
                    admin.setLastName("Saik");
                    admin.setEmail("admin@saik.fr");
                    admin.setPassword("admin123"); // À encoder avec BCrypt lors de l'ajout de Spring Security
                    admin.setRoles(Set.of(adminRole));
                    userRepo.save(admin);
                }
            }

            if (countryRepo.count() == 0) {
                countryRepo.save(new Country("France", "FR"));
                countryRepo.save(new Country("Japon", "JP"));
                countryRepo.save(new Country("Espagne", "ES"));
            }
        };
    }
}