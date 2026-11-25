package projet.saik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import projet.saik.repository.BudgetRepository;
import projet.saik.repository.TripRepository;
import projet.saik.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iris-demo-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "saik.iris-demo-password=iris-test-password"
})
@AutoConfigureMockMvc
// Vérifie l’initialisation conditionnelle du compte et du voyage Iris de démonstration.
class IrisDemoTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired BudgetRepository budgets;
    @Autowired PasswordEncoder passwords;
    @Autowired @Qualifier("initIrisDemo") CommandLineRunner initializer;

    @Test
    // Confirme la création initiale, la connexion et l’absence de réinitialisation au redémarrage.
    void createsUsableAccountAndPreservesItOnRestart() throws Exception {
        // Vérifie les données du compte et l’encodage du mot de passe.
        var iris = users.findByEmailIgnoreCase("iris.geyer@email.com").orElseThrow();
        assertEquals("Iris", iris.getFirstName());
        assertEquals("Geyer", iris.getLastName());
        assertTrue(passwords.matches("iris-test-password", iris.getPassword()));
        assertFalse(iris.getPassword().equals("iris-test-password"));

        // Vérifie qu’un seul voyage en Chine et son budget ont été créés.
        var savedTrips = trips.findByUserIdOrderByStartDateAsc(iris.getId());
        assertEquals(1, savedTrips.size());
        var trip = savedTrips.get(0);
        assertEquals("Chine — Découverte & Aventure", trip.getName());
        assertEquals(LocalDate.of(2026, 3, 1), trip.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 21), trip.getEndDate());
        assertTrue(budgets.findByTripId(trip.getId()).isPresent());

        // Connecte le compte de démonstration avec le formulaire web.
        var login = mvc.perform(post("/login").with(csrf())
                .param("username", "iris.geyer@email.com").param("password", "iris-test-password"))
                .andExpect(redirectedUrl("/")).andReturn();
        // Vérifie que le profil et le voyage sont accessibles dans la session authentifiée.
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/profile").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Iris"))
                .andExpect(jsonPath("$.lastName").value("Geyer"));
        mvc.perform(get("/api/trips").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("Chine"))
                .andExpect(jsonPath("$[0].iso").value("CN"))
                .andExpect(jsonPath("$[0].start").value("2026-03-01"))
                .andExpect(jsonPath("$[0].end").value("2026-03-21"));

        // Modifie le compte puis relance l’initialiseur pour vérifier qu’il ne l’écrase pas.
        iris.setFirstName("Iris modifié");
        users.saveAndFlush(iris);
        initializer.run();
        assertEquals("Iris modifié", users.findById(iris.getId()).orElseThrow().getFirstName());
        assertEquals(1, trips.findByUserIdOrderByStartDateAsc(iris.getId()).size());
        assertTrue(passwords.matches("iris-test-password", users.findById(iris.getId()).orElseThrow().getPassword()));
    }
}
