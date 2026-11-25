package projet.saik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import projet.saik.config.AdminInitializer;
import projet.saik.repository.RoleRepository;
import projet.saik.repository.UserRepository;
import projet.saik.service.AccountService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saik-admin-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "saik.security.max-login-attempts=3"
})
@AutoConfigureMockMvc
// Vérifie les accès administrateur, le blocage des connexions et la confirmation du changement d’e-mail.
class AdminSecurityTests {
    private static final String PASSWORD = "password-admin-test";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired AccountService accounts;
    @Autowired TransactionTemplate transactions;

    @Test
    // Un simple utilisateur reçoit 403 sur les routes admin, un administrateur y accède.
    void onlyAdminsReachAdminRoutes() throws Exception {
        register("plain@example.test");
        register("boss@example.test");
        promoteToAdmin("boss@example.test");
        String plain = login("plain@example.test", PASSWORD);
        String boss = login("boss@example.test", PASSWORD);

        // Sans jeton : refusé.
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        // Avec un jeton d’utilisateur ordinaire : interdit, en lecture comme en suppression.
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(plain))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/users/1").header("Authorization", bearer(plain))).andExpect(status().isForbidden());

        // L’administrateur voit les comptes, sans mot de passe.
        String json = mvc.perform(get("/api/admin/users").header("Authorization", bearer(boss)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long bossId = idOf(json, "boss@example.test");
        long plainId = idOf(json, "plain@example.test");

        // Il ne peut pas supprimer son propre compte, mais peut supprimer un autre compte.
        mvc.perform(delete("/api/admin/users/" + bossId).header("Authorization", bearer(boss)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/users/" + plainId).header("Authorization", bearer(boss)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/users/" + plainId).header("Authorization", bearer(boss)))
                .andExpect(status().isNotFound());
        // Le compte supprimé ne peut plus se connecter.
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("plain@example.test", PASSWORD))).andExpect(status().isUnauthorized());
    }

    @Test
    // La même protection s’applique aux sessions web.
    void adminRoutesAreProtectedForSessionsToo() throws Exception {
        register("web-plain@example.test");
        var result = mvc.perform(post("/login").with(csrf()).param("username", "web-plain@example.test")
                .param("password", PASSWORD)).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isForbidden());
    }

    @Test
    // Après trop d’échecs le compte est bloqué ; une connexion réussie remet le compteur à zéro.
    void repeatedFailuresLockTheAccount() throws Exception {
        register("locked@example.test");
        // Deux échecs puis un succès : le compteur est remis à zéro...
        fail("locked@example.test");
        fail("locked@example.test");
        login("locked@example.test", PASSWORD);
        // ... donc deux nouveaux échecs ne bloquent pas encore le compte.
        fail("locked@example.test");
        fail("locked@example.test");
        login("locked@example.test", PASSWORD);
        // Trois échecs de suite bloquent le compte, même avec le bon mot de passe.
        fail("locked@example.test");
        fail("locked@example.test");
        fail("locked@example.test");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("locked@example.test", PASSWORD))).andExpect(status().isUnauthorized());
        // Un autre compte n’est pas affecté.
        register("free@example.test");
        login("free@example.test", PASSWORD);
    }

    @Test
    // Changer d’adresse e-mail exige le mot de passe actuel.
    void changingEmailRequiresCurrentPassword() throws Exception {
        register("mail@example.test");
        String token = login("mail@example.test", PASSWORD);
        String base = "\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"new-mail@example.test\"";

        mvc.perform(put("/api/profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{" + base + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{" + base + ",\"currentPassword\":\"faux-mot-de-passe\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{" + base + ",\"currentPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new-mail@example.test"));
        // Sans changement d’adresse, aucun mot de passe n’est demandé.
        mvc.perform(put("/api/profile").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"C\",\"lastName\":\"D\",\"email\":\"new-mail@example.test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    // L’initialiseur crée l’administrateur, puis ne fait rien de plus s’il est relancé.
    void initializerCreatesAndPromotesAdmin() throws Exception {
        var initializer = new AdminInitializer();
        // Sans e-mail configuré, rien n’est créé.
        initializer.initAdmin("", "", accounts, users, roles, transactions).run();
        assertTrue(users.findByEmail("init-admin@example.test").isEmpty());

        initializer.initAdmin("init-admin@example.test", PASSWORD, accounts, users, roles, transactions).run();
        var admin = users.findByEmail("init-admin@example.test").orElseThrow();
        assertTrue(admin.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName())));
        int roleCount = admin.getRoles().size();

        // Relancé, il ne duplique ni le compte ni le rôle.
        initializer.initAdmin("init-admin@example.test", PASSWORD, accounts, users, roles, transactions).run();
        assertEquals(roleCount, users.findByEmail("init-admin@example.test").orElseThrow().getRoles().size());
    }

    private void register(String email) throws Exception {
        // L’inscription exige un prénom et un nom en plus des identifiants.
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Test\",\"lastName\":\"User\","
                        + "\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private void fail(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, "mauvais-mot-de-passe"))).andExpect(status().isUnauthorized());
    }

    private void promoteToAdmin(String email) {
        transactions.executeWithoutResult(status -> {
            var user = users.findByEmail(email).orElseThrow();
            user.getRoles().add(roles.findByName("ROLE_ADMIN").orElseThrow());
            users.save(user);
        });
    }

    private long idOf(String json, String email) {
        List<Number> ids = JsonPath.read(json, "$[?(@.email=='" + email + "')].id");
        return ids.get(0).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
