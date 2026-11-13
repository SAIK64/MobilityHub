package projet.saik.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionTemplate;

import projet.saik.model.Budget;
import projet.saik.model.Country;
import projet.saik.model.Trip;
import projet.saik.repository.CountryRepository;
import projet.saik.repository.TripRepository;
import projet.saik.repository.UserRepository;
import projet.saik.service.AccountService;

@Configuration
// Crée un compte et un voyage de démonstration seulement si un mot de passe est configuré.
public class IrisDemoInitializer {
    // Le journal confirme les décisions de l’initialiseur sans afficher le mot de passe.
    private static final Logger log = LoggerFactory.getLogger(IrisDemoInitializer.class);
    // Adresse stable utilisée pour reconnaître le compte de démonstration.
    private static final String EMAIL = "iris.geyer@email.com";

    @Bean
    @Order(2)
    // Fournit les dépôts et services nécessaires à la création des données de démonstration.
    CommandLineRunner initIrisDemo(
            @Value("${saik.iris-demo-password:}") String password,
            AccountService accounts,
            UserRepository users,
            CountryRepository countries,
            TripRepository trips,
            TransactionTemplate transactions) {
        return args -> {
            // Sans secret configuré, ne crée aucun compte de démonstration.
            if (password.isEmpty()) {
                return;
            }
            // Regroupe la création en transaction pour éviter un compte ou voyage partiellement créé.
            transactions.executeWithoutResult(status -> {
                // Ne modifie jamais un compte Iris déjà présent dans la base.
                if (users.findByEmailIgnoreCase(EMAIL).isPresent()) {
                    log.info("Le compte Iris existe déjà : aucune donnée de démonstration n'est modifiée.");
                    return;
                }
                // Crée le compte avec le mécanisme d’inscription qui encode le mot de passe.
                var iris = accounts.register(EMAIL, password, "Iris", "Geyer");
                // Remplit les informations de profil de démonstration.
                iris.setBirthDate("14 Mai 2004");
                iris.setNationality("Française");
                iris.setPhone("+33 6 12 34 56 78");
                iris.setPassport("Valide jusqu'en 2032");
                users.save(iris);

                // Réutilise la Chine si elle existe, sinon l’ajoute au catalogue.
                Country china = countries.findByCodeIso("CN")
                        .orElseGet(() -> countries.save(new Country("Chine", "CN")));
                // Prépare le séjour associé au compte Iris.
                Trip trip = new Trip();
                trip.setName("Chine — Découverte & Aventure");
                trip.setStartDate(LocalDate.of(2026, 3, 1));
                trip.setEndDate(LocalDate.of(2026, 3, 21));
                trip.setUser(iris);
                trip.getCountries().add(china);
                // Crée aussi un budget vide pour le séjour.
                trip.setBudget(new Budget());
                trips.save(trip);
                // Confirme la création sans exposer de données d’authentification.
                log.info("Le compte de démonstration Iris et son voyage en Chine ont été créés.");
            });
        };
    }
}
