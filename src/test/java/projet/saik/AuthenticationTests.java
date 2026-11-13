package projet.saik;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import projet.saik.config.LegacyPasswordMigration;
import projet.saik.model.User;
import projet.saik.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saik-auth-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
// Vérifie les parcours de connexion, d’inscription, de sécurité web et d’API.
class AuthenticationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired TransactionTemplate transactions;

    @Test
    void homeIsPublicAndLinksToAuthentication() throws Exception {
        for (String path : new String[] { "/", "/accueil" }) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(view().name("accueil"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/login\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/register\"")));
        }
        for (String path : new String[] { "/login", "/register" }) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(
                            "href=\"/accueil\">← Retour à l’accueil</a>")));
        }
        mvc.perform(get("/css/accueil.css")).andExpect(status().isOk());
    }

    @Test
    void connectedPagesShareNavigationAndDesign() throws Exception {
        for (String path : new String[] { "/dashboard", "/transports", "/budget", "/securite", "/profil" }) {
            mvc.perform(get(path).with(user("voyageur@example.test")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/design.css\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"/js/design-tokens.js\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Navigation principale\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"" + path + "\" data-path=")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-current=\"page\"")));
        }
        mvc.perform(get("/css/design.css")).andExpect(status().isOk());
        mvc.perform(get("/js/design-tokens.js")).andExpect(status().isOk());
    }

    @Test
    void pagesDoNotPresentDemoDataAsUserData() throws Exception {
        for (String path : new String[] { "/dashboard", "/transports", "/budget", "/profil" }) {
            mvc.perform(get(path).with(user("voyageur@example.test")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Iris Geyer"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Voyage Chine 2026"))));
        }
        mvc.perform(get("/profil").with(user("voyageur@example.test")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-profile-age")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"birthDate\"")));
    }

    @Test
    // Vérifie l’inscription par formulaire, la session, le CSRF et la déconnexion.
    void registrationLoginCsrfAndLogout() throws Exception {
        // Une personne non connectée est redirigée depuis les pages privées.
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/trips")).andExpect(status().isUnauthorized());
        mvc.perform(get("/h2-console/")).andExpect(status().is3xxRedirection());
        // Les pages d’authentification restent accessibles et affichent les bons formulaires.
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(
                org.hamcrest.Matchers.containsString("Se connecter")));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(
                org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        // Le formulaire refuse les champs invalides et exige le jeton CSRF.
        mvc.perform(post("/register").with(csrf()).param("email", "invalide")
                .param("password", "court").param("firstName", "Jean").param("lastName", "Dupont"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Adresse e-mail invalide")));
        mvc.perform(post("/register").with(csrf()).param("email", "missing-name@example.test")
                .param("password", "password-web")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Le prénom est obligatoire.")));
        mvc.perform(post("/register").param("email", "WEB@example.test")
                .param("password", "password-web")).andExpect(status().isForbidden());
        // Une inscription valide crée le compte puis redirige vers la connexion.
        mvc.perform(post("/register").with(csrf()).param("email", "WEB@example.test")
                .param("password", "password-web").param("firstName", "  Jean  ").param("lastName", "Dupont"))
                .andExpect(redirectedUrl("/login?registered"));

        // Vérifie que le mot de passe sauvegardé est encodé et peut être vérifié par BCrypt.
        var stored = users.findByEmail("web@example.test").orElseThrow();
        assertEquals("Jean", stored.getFirstName());
        assertEquals("Dupont", stored.getLastName());
        assertNotEquals("password-web", stored.getPassword());
        assertTrue(passwords.matches("password-web", stored.getPassword()));

        // Une connexion avec un mauvais mot de passe échoue, contrairement aux bons identifiants.
        mvc.perform(post("/login").with(csrf()).param("username", "web@example.test")
                .param("password", "incorrect")).andExpect(redirectedUrl("/login?error"));
        var login = mvc.perform(post("/login").with(csrf()).param("username", "WEB@example.test")
                .param("password", "password-web")).andExpect(redirectedUrl("/")).andReturn();
        // Réutilise la session obtenue après connexion pour accéder aux pages privées.
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
        mvc.perform(get("/dashboard").session(session)).andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
        mvc.perform(get("/securite").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-selected-trip")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Les numéros de secours varient selon le pays")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("tel:119"))));
        mvc.perform(get("/h2-console/").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/trips").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(tripJson("Voyage web"))).andExpect(status().isForbidden());

        // Récupère le jeton CSRF requis pour les opérations qui modifient les données.
        String csrfJson = mvc.perform(get("/api/csrf").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // Vérifie la création d’un voyage et la mise à jour du profil avec le jeton reçu.
        String header = JsonPath.read(csrfJson, "$.headerName");
        String token = JsonPath.read(csrfJson, "$.token");
        mvc.perform(post("/api/trips").session(session).header(header, token)
                .contentType(MediaType.APPLICATION_JSON).content(tripJson("Voyage web")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/profile").session(session).header(header, token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Web","lastName":"User","email":"web-changed@example.test","birthDate":"2000-02-29","nationality":"Française"}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/profile").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("web-changed@example.test"))
                .andExpect(jsonPath("$.birthDate").value("2000-02-29"))
                .andExpect(jsonPath("$.nationality").value("Française"));
        // Après la déconnexion, la session ne donne plus accès aux données protégées.
        String parameter = JsonPath.read(csrfJson, "$.parameterName");
        mvc.perform(post("/logout").session(session).param(parameter, token)).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/api/trips").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    // Vérifie l’émission des JWT et l’isolation des données entre deux comptes.
    void bearerRoutesIsolateUsersAndSurviveEmailChange() throws Exception {
        // Crée deux comptes indépendants pour comparer leurs droits d’accès.
        registerApi("first@example.test");
        registerApi("second@example.test");
        // Refuse les doublons d’adresse, les mots de passe trop longs et les identifiants incorrects.
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"First\",\"lastName\":\"User\","
                        + "\"email\":\"FIRST@example.test\",\"password\":\"password-api\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Long\",\"lastName\":\"User\","
                        + "\"email\":\"long@example.test\",\"password\":\"" + "é".repeat(37) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"first@example.test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        // Le premier compte crée un voyage auquel le second compte ne doit pas accéder.
        String first = loginApi("first@example.test");
        String second = loginApi("second@example.test");
        // Le propriétaire voit ses propres données, alors que le second compte ne peut ni les lire ni les modifier.
        String created = mvc.perform(post("/api/trips").header("Authorization", "Bearer " + first)
                .contentType(MediaType.APPLICATION_JSON).content(tripJson("Voyage privé")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer tripId = JsonPath.read(created, "$.id");
        mvc.perform(get("/api/trips").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/trips/" + tripId + "/tickets").header("Authorization", "Bearer " + second))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/trips/" + tripId + "/tickets").header("Authorization", "Bearer " + second)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"type":"Train","description":"Test","departureTime":"2030-01-01T10:00:00","price":10}
                """)).andExpect(status().isNotFound());
        String ticket = mvc.perform(post("/api/trips/" + tripId + "/tickets")
                .header("Authorization", "Bearer " + first).contentType(MediaType.APPLICATION_JSON).content("""
                {"type":"Train","description":"Test","departureTime":"2030-01-01T10:00:00","price":10}
                """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer ticketId = JsonPath.read(ticket, "$.id");
        String expenses = mvc.perform(get("/api/trips/" + tripId + "/expenses")
                .header("Authorization", "Bearer " + first))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer expenseId = JsonPath.read(expenses, "$[0].id");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/tickets/" + ticketId)
                .header("Authorization", "Bearer " + second)).andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/expenses/" + expenseId)
                .header("Authorization", "Bearer " + second)).andExpect(status().isNotFound());
        // Vérifie qu’un changement d’adresse ne rend pas le jeton actif inutilisable.
        mvc.perform(put("/api/profile").header("Authorization", "Bearer " + first)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Test","lastName":"User","email":"changed@example.test"}
                """)).andExpect(status().isOk());
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("changed@example.test"));
        mvc.perform(get("/api/trips").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    // Vérifie que l’ancien compte de démonstration ne conserve pas son mot de passe faible.
    void legacyDemoMustBeResetBeforeItCanBeUsed() throws Exception {
        // Reproduit un compte historique avec son ancien mot de passe non encodé.
        User legacy = new User();
        legacy.setFirstName("Admin");
        legacy.setLastName("Saik");
        legacy.setEmail("admin@saik.fr");
        legacy.setPassword("admin123");
        users.saveAndFlush(legacy);
        // Sans nouveau secret de démonstration, la migration doit échouer sans modifier le compte.
        var migration = new LegacyPasswordMigration();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> migration.migrateLegacyPasswords(users, passwords, transactions, "").run());
        assertTrue(exception.getMessage().contains("SAIK_LEGACY_DEMO_PASSWORD"));
        assertEquals("admin123", users.findByEmail("admin@saik.fr").orElseThrow().getPassword());
        // Avec un secret conforme, la migration remplace l’ancien mot de passe par une empreinte BCrypt.
        migration.migrateLegacyPasswords(users, passwords, transactions, "new-demo-password").run();
        String hash = users.findByEmail("admin@saik.fr").orElseThrow().getPassword();
        assertTrue(passwords.matches("new-demo-password", hash));
        assertTrue(!passwords.matches("admin123", hash));
        users.deleteById(legacy.getId());
    }

    private void registerApi(String email) throws Exception {
        // Inscrit un compte via l’API de test en envoyant une adresse variable.
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Test\",\"lastName\":\"User\","
                        + "\"email\":\"" + email + "\",\"password\":\"password-api\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    private String loginApi(String email) throws Exception {
        // Se connecte via l’API puis extrait le jeton Bearer de la réponse JSON.
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password-api\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String tripJson(String name) {
        // Construit le JSON minimal utilisé par les tests de création de voyage.
        return "{\"name\":\"" + name + "\",\"country\":\"France\",\"iso\":\"FR\","
                + "\"start\":\"2030-01-01\",\"end\":\"2030-01-03\"}";
    }
}
