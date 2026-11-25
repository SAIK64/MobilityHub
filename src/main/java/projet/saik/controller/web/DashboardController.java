package projet.saik.controller.web;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import projet.saik.service.TravelDataService;
import projet.saik.web.TravelDataDtos.CountryRequest;
import projet.saik.web.TravelDataDtos.TripRequest;
import projet.saik.web.TripView;

@Controller
// Tableau de bord : liste, création, modification et suppression des voyages,
// et association / dissociation des pays d'un voyage (relation N-N Trip <-> Country).
public class DashboardController {

    private final TravelDataService service;
    private final WebActions actions;

    public DashboardController(TravelDataService service, WebActions actions) {
        this.service = service;
        this.actions = actions;
    }

    @GetMapping({ "/", "/dashboard" })
    public String dashboard(Principal principal, Model model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long edit) {
        if (principal == null) {
            return "accueil";
        }
        // « trips » et « selectedTrip » sont fournis par LayoutAdvice.
        @SuppressWarnings("unchecked")
        List<TripView> trips = (List<TripView>) model.getAttribute("trips");

        // Compteurs des onglets de filtre.
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Tous", (long) trips.size());
        for (String s : List.of(TripView.ACTIVE, TripView.FUTURE, TripView.ARCHIVED)) {
            counts.put(s, trips.stream().filter(t -> t.getStatus().equals(s)).count());
        }
        String activeFilter = counts.containsKey(status) ? status : "Tous";
        model.addAttribute("counts", counts);
        model.addAttribute("activeFilter", activeFilter);
        model.addAttribute("filteredTrips", "Tous".equals(activeFilter) ? trips
                : trips.stream().filter(t -> t.getStatus().equals(activeFilter)).toList());

        // Une ligne « pays » vide pour le formulaire de création.
        model.addAttribute("emptyCountryRow", java.util.Collections.singletonList(null));
        // Voyage à modifier : le formulaire s'ouvre pré-rempli.
        if (edit != null) {
            trips.stream().filter(t -> t.getId().equals(edit)).findFirst()
                    .ifPresent(trip -> model.addAttribute("editTrip", trip));
        }
        return "dashboard";
    }

    @PostMapping("/dashboard/trips")
    public String createTrip(@RequestParam String name,
            @RequestParam(name = "countryName", required = false) List<String> countryNames,
            @RequestParam(name = "countryIso", required = false) List<String> countryIsos,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpSession session, RedirectAttributes flash) {
        actions.run(flash, "Voyage enregistré.", () -> {
            TripRequest request = actions.validate(
                    new TripRequest(name, null, null, toCountries(countryNames, countryIsos), start, end));
            Long id = service.createTrip(request).id();
            // Le nouveau voyage devient le voyage sélectionné.
            session.setAttribute(SelectedTripResolver.SESSION_KEY, id);
        });
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/trips/{tripId}")
    public String updateTrip(@PathVariable Long tripId, @RequestParam String name,
            @RequestParam(name = "countryName", required = false) List<String> countryNames,
            @RequestParam(name = "countryIso", required = false) List<String> countryIsos,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            RedirectAttributes flash) {
        boolean ok = actions.run(flash, "Voyage mis à jour.", () -> service.updateTrip(tripId, actions.validate(
                new TripRequest(name, null, null, toCountries(countryNames, countryIsos), start, end))));
        return ok ? "redirect:/dashboard" : "redirect:/dashboard?edit=" + tripId;
    }

    @PostMapping("/dashboard/trips/{tripId}/delete")
    public String deleteTrip(@PathVariable Long tripId, RedirectAttributes flash) {
        actions.run(flash, "Voyage supprimé.", () -> service.deleteTrip(tripId));
        return "redirect:/dashboard";
    }

    // Associe un pays à un voyage (N-N).
    @PostMapping("/dashboard/trips/{tripId}/countries")
    public String addCountry(@PathVariable Long tripId, @RequestParam String country, @RequestParam String iso,
            RedirectAttributes flash) {
        actions.run(flash, country.trim() + " ajouté au voyage.", () -> service.addCountry(tripId,
                actions.validate(new CountryRequest(country.trim(), iso.trim().toUpperCase()))));
        return "redirect:/dashboard";
    }

    // Dissocie un pays d'un voyage (N-N).
    @PostMapping("/dashboard/trips/{tripId}/countries/{iso}/delete")
    public String removeCountry(@PathVariable Long tripId, @PathVariable String iso, RedirectAttributes flash) {
        actions.run(flash, "Pays retiré du voyage.", () -> service.removeCountry(tripId, iso));
        return "redirect:/dashboard";
    }

    // Les lignes « pays » du formulaire arrivent sous forme de deux listes parallèles.
    private static List<CountryRequest> toCountries(List<String> names, List<String> isos) {
        List<CountryRequest> countries = new ArrayList<>();
        if (names == null || isos == null) {
            return countries;
        }
        for (int i = 0; i < Math.min(names.size(), isos.size()); i++) {
            countries.add(new CountryRequest(names.get(i).trim(), isos.get(i).trim().toUpperCase()));
        }
        return countries;
    }
}
