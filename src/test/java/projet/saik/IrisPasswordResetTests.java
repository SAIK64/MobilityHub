package projet.saik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import projet.saik.repository.TripRepository;
import projet.saik.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iris-reset-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Le compte est d'abord créé avec un premier mot de passe…
        "saik.iris-demo-password=ancien-mot-de-passe",
        // … puis réinitialisé au même démarrage.
        "saik.iris-reset-password=nouveau-mot-de-passe"
})
@AutoConfigureMockMvc
// Vérifie la réinitialisation du mot de passe Iris via SAIK_IRIS_RESET_PASSWORD.
class IrisPasswordResetTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;
    @Autowired PasswordEncoder passwords;

    @Test
    void resetsPasswordAndKeepsTrips() throws Exception {
        var iris = users.findByEmailIgnoreCase("iris.geyer@email.com").orElseThrow();
        // Seul le nouveau mot de passe est valide, et il est stocké en BCrypt.
        assertTrue(passwords.matches("nouveau-mot-de-passe", iris.getPassword()));
        assertFalse(passwords.matches("ancien-mot-de-passe", iris.getPassword()));
        // Les données du compte sont conservées.
        assertEquals(1, trips.findByUserIdOrderByStartDateAsc(iris.getId()).size());

        mvc.perform(post("/login").with(csrf())
                .param("username", "iris.geyer@email.com").param("password", "nouveau-mot-de-passe"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(post("/login").with(csrf())
                .param("username", "iris.geyer@email.com").param("password", "ancien-mot-de-passe"))
                .andExpect(redirectedUrl("/login?error"));
    }
}
