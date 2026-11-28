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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import projet.saik.repository.TransportTicketRepository;
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
    @Autowired TransportTicketRepository ticketRepository;

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

        // Recherche en base : par nom de voyage, par nom de pays (jointure N-N), par code ISO, sans casse.
        mvc.perform(post("/dashboard/trips").session(session).with(csrf())
                .param("name", "Vacances Japon").param("countryName", "Japon").param("countryIso", "JP")
                .param("start", "2031-04-01").param("end", "2031-04-15"))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(get("/dashboard").param("q", "road").session(session))
                .andExpect(model().attribute("q", "road"))
                .andExpect(content().string(allOf(containsString("1 résultat(s)"), containsString("Road trip Europe"),
                        not(containsString("Vacances Japon</h4>")))));
        mvc.perform(get("/dashboard").param("q", "allemagne").session(session))
                .andExpect(content().string(allOf(containsString("Road trip Europe</h4>"), not(containsString("Vacances Japon</h4>")))));
        mvc.perform(get("/dashboard").param("q", "jp").session(session))
                .andExpect(content().string(allOf(containsString("Vacances Japon</h4>"), not(containsString("Road trip Europe</h4>")))));
        mvc.perform(get("/dashboard").param("q", "pérou").session(session))
                .andExpect(content().string(containsString("Aucun voyage ne correspond à votre recherche.")));
        mvc.perform(post("/dashboard/trips/" + trips.findByUserIdOrderByStartDateAsc(userId).get(1).getId() + "/delete")
                .session(session).with(csrf())).andExpect(redirectedUrl("/dashboard"));

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

        // Billet de train créé par le formulaire MVC (TicketForm) : apparaît sur la page transports
        // et génère automatiquement une dépense « Transport ».
        mvc.perform(post("/transports/tickets").session(session).with(csrf())
                .param("type", "Train").param("description", "Paris → Berlin")
                .param("departure", "Paris Est").param("arrival", "Berlin Hbf")
                .param("operatorName", "SNCF").param("serviceNumber", "ICE 9553").param("seatInfo", "Voiture 12, place 45")
                .param("departureTime", "2030-06-01T08:00").param("arrivalTime", "2030-06-01T16:30")
                .param("price", "100"))
                .andExpect(redirectedUrl("/transports"))
                .andExpect(flash().attribute("message", "Titre enregistré."));
        Long ticketId = ticketRepository.findByTripIdOrderByDepartureTimeAsc(tripId).get(0).getId();

        // Formulaire de modification pré-rempli avec les valeurs enregistrées.
        mvc.perform(get("/transports").param("edit", ticketId.toString()).session(session))
                .andExpect(model().attribute("editTicketId", ticketId))
                .andExpect(content().string(allOf(containsString("Modifier le titre de transport"),
                        containsString("value=\"SNCF\""), containsString("value=\"2030-06-01T08:00\""))));

        // Modification : nouveau prix -> la dépense générée suit.
        mvc.perform(post("/transports/tickets/" + ticketId).session(session).with(csrf())
                .param("type", "Train").param("description", "Paris → Berlin")
                .param("departure", "Paris Est").param("arrival", "Berlin Hbf")
                .param("operatorName", "SNCF").param("serviceNumber", "ICE 9553").param("seatInfo", "Voiture 14")
                .param("departureTime", "2030-06-01T08:00").param("price", "120.5"))
                .andExpect(redirectedUrl("/transports"))
                .andExpect(flash().attribute("message", "Titre modifié."));
        mvc.perform(get("/transports").session(session))
                .andExpect(view().name("transports")).andExpect(model().attributeExists("tickets", "ticketForm"))
                .andExpect(content().string(allOf(containsString("Paris → Berlin"), containsString("120,50 €"),
                        containsString("ICE 9553"), containsString("Voiture 14"))));

        // Erreur de saisie (vol sans PNR ni détails) : la page est réaffichée avec le formulaire ouvert et les valeurs.
        mvc.perform(post("/transports/tickets").session(session).with(csrf())
                .param("type", "Avion").param("description", "Vol incomplet")
                .param("departureTime", "2030-06-02T10:00").param("price", "300"))
                .andExpect(status().isOk()).andExpect(view().name("transports"))
                .andExpect(model().attribute("openTicketForm", true))
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(containsString("value=\"Vol incomplet\"")));

        // Billet d'avion complet par formulaire : le service calcule la durée (8 h 30).
        mvc.perform(post("/transports/tickets").session(session).with(csrf())
                .param("type", "Avion").param("description", "Vol AF 123")
                .param("departureTime", "2030-06-03T10:00").param("arrivalTime", "2030-06-03T12:30")
                .param("price", "0").param("reference", "a1b2c3")
                .param("flight.departureAirport", "Paris-CDG").param("flight.departureIata", "cdg")
                .param("flight.arrivalAirport", "New York-JFK").param("flight.arrivalIata", "JFK")
                .param("flight.stopCount", "0").param("flight.airline", "Air France")
                .param("flight.fareType", "Economy Standard").param("flight.ticketStatus", "Confirmé / OK")
                .param("flight.departureUtcOffsetMinutes", "60").param("flight.arrivalUtcOffsetMinutes", "-300")
                .param("flight.eticketNumber", "0571234567890"))
                .andExpect(redirectedUrl("/transports"));
        mvc.perform(get("/transports").session(session))
                .andExpect(content().string(allOf(containsString("CDG → JFK"), containsString("Durée : 8h 30min"))));

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

        // L'âge est aussi calculé pour une date écrite en français (format des données de démonstration).
        mvc.perform(post("/profil").session(session).with(csrf())
                .param("firstName", "Marie").param("lastName", "Modèle").param("email", "mvc@example.test")
                .param("birthDate", "05 mars 2005"))
                .andExpect(redirectedUrl("/profil"));
        mvc.perform(get("/profil").session(session))
                .andExpect(model().attribute("age", java.time.Period.between(
                        java.time.LocalDate.of(2005, 3, 5), java.time.LocalDate.now()).getYears()));

        // Suppression du voyage.
        mvc.perform(post("/dashboard/trips/" + tripId + "/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl("/dashboard"));
        assertEquals(0, trips.findByUserIdOrderByStartDateAsc(userId).size());
    }
}
