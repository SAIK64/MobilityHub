package projet.saik;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import projet.saik.repository.TripRepository;
import projet.saik.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:saik-web-mvc-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
// Vérifie le MVC des pages : chaque contrôleur transmet ses données au modèle,
// la vue les affiche, et les formulaires POST modifient la base puis redirigent.
class WebMvcPagesTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TripRepository trips;

    private MockHttpSession login() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("email", "mvc@example.test")
                .param("password", "password-mvc").param("firstName", "Marie").param("lastName", "Vue"));
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("username", "mvc@example.test").param("password", "password-mvc"))
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void pagesDisplayControllerDataAndFormsUpdateDatabase() throws Exception {
        MockHttpSession session = login();

        // Création d'un voyage par formulaire (deux pays -> relation N-N).
        mvc.perform(post("/dashboard/trips").session(session).with(csrf())
                .param("name", "Road trip Europe")
                .param("countryName", "France", "Allemagne")
                .param("countryIso", "fr", "DE")
                .param("start", "2030-06-01").param("end", "2030-06-10"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("message", "Voyage enregistré."));
        Long userId = users.findByEmail("mvc@example.test").orElseThrow().getId();
        Long tripId = trips.findByUserIdOrderByStartDateAsc(userId).get(0).getId();

        // Le tableau de bord affiche les données transmises par le contrôleur.
        mvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk()).andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("trips", "selectedTrip", "counts", "filteredTrips"))
                .andExpect(content().string(allOf(
                        containsString("Road trip Europe"), containsString("France (FR)"),
                        containsString("Allemagne (DE)"), containsString("10 jours"),
                        containsString("Départ dans"))));

        // Filtre par statut : un voyage futur n'est pas « Archivé ».
        mvc.perform(get("/dashboard").param("status", "Archivé").session(session))
                .andExpect(content().string(containsString("Aucun voyage dans cette catégorie.")));

        // Association puis dissociation d'un pays (N-N).
        mvc.perform(post("/dashboard/trips/" + tripId + "/countries").session(session).with(csrf())
                .param("country", "Italie").param("iso", "it"))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/dashboard/trips/" + tripId + "/countries/DE/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(get("/dashboard").session(session))
                .andExpect(content().string(allOf(containsString("Italie (IT)"), not(containsString("Allemagne (DE)")))));

        // Erreur métier : la date de fin précède le départ -> message d'erreur, rien n'est créé.
        mvc.perform(post("/dashboard/trips").session(session).with(csrf())
                .param("name", "Invalide").param("countryName", "Espagne").param("countryIso", "ES")
                .param("start", "2030-06-10").param("end", "2030-06-01"))
                .andExpect(flash().attributeExists("error"));
        assertEquals(1, trips.findByUserIdOrderByStartDateAsc(userId).size());

        // Formulaire de modification pré-rempli.
        mvc.perform(get("/dashboard").param("edit", tripId.toString()).session(session))
                .andExpect(model().attributeExists("editTrip"))
                .andExpect(content().string(containsString("Enregistrer les modifications")));

        // Un billet payant (créé via l'API) apparaît sur la page transports rendue par le serveur
        // et génère automatiquement une dépense « Transport ».
        mvc.perform(post("/api/trips/" + tripId + "/tickets").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type":"Train","description":"Paris → Berlin","departure":"Paris","arrival":"Berlin",
                         "departureTime":"2030-06-01T08:00","arrivalTime":"2030-06-01T16:30","price":120.5}
                        """))
                .andExpect(status().isOk());
        mvc.perform(get("/transports").session(session))
                .andExpect(view().name("transports")).andExpect(model().attributeExists("tickets"))
                .andExpect(content().string(allOf(containsString("Paris → Berlin"), containsString("120,50 €"),
                        containsString("data-selected-trip-id=\"" + tripId + "\""))));

        // Dépense manuelle + budgets par catégorie via formulaires.
        mvc.perform(post("/budget/expenses").session(session).with(csrf())
                .param("tripId", tripId.toString()).param("description", "Hôtel Berlin")
                .param("amount", "300").param("date", "2030-06-02").param("category", "Hébergement"))
                .andExpect(redirectedUrl("/budget"));
        mvc.perform(post("/budget/categories").session(session).with(csrf())
                .param("tripId", tripId.toString()).param("limit_Hébergement", "250").param("limit_Transport", ""))
                .andExpect(redirectedUrl("/budget"));
        mvc.perform(get("/budget").session(session))
                .andExpect(view().name("budget")).andExpect(model().attributeExists("budget", "expenses"))
                .andExpect(content().string(allOf(containsString("Hôtel Berlin"), containsString("Paris → Berlin"),
                        containsString("420,50 €"), containsString("dépassé de 50,00 €"))));

        // Recherche / filtre des dépenses côté serveur.
        mvc.perform(get("/budget").param("type", "AUTO").session(session))
                .andExpect(content().string(allOf(containsString("Paris → Berlin"), not(containsString("Hôtel Berlin")))));
        mvc.perform(get("/budget").param("q", "hôtel").session(session))
                .andExpect(content().string(allOf(containsString("Hôtel Berlin"), not(containsString("Automatique ·")))));

        // Profil : affichage pré-rempli puis modification par formulaire.
        mvc.perform(post("/profil").session(session).with(csrf())
                .param("firstName", "Marie").param("lastName", "Modèle").param("email", "mvc@example.test")
                .param("birthDate", "2000-01-15").param("nationality", "Française"))
                .andExpect(redirectedUrl("/profil"));
        mvc.perform(get("/profil").session(session))
                .andExpect(model().attributeExists("profile", "age"))
                .andExpect(content().string(allOf(containsString("Marie Modèle"), containsString("value=\"Française\""))));

        // Suppression du voyage.
        mvc.perform(post("/dashboard/trips/" + tripId + "/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
        assertEquals(0, trips.findByUserIdOrderByStartDateAsc(userId).size());
    }
}
