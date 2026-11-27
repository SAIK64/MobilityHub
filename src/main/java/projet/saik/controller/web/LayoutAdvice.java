package projet.saik.controller.web;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import projet.saik.service.TripService;
import projet.saik.model.CountryIso;
import projet.saik.survival.EmergencyDirectory;
import projet.saik.survival.EmergencyInfo;
import java.util.Map;
import java.util.LinkedHashMap;
import projet.saik.web.TravelDataDtos.TripResponse;
import projet.saik.web.TripView;

// Ajoute à toutes les pages du package web les données communes au gabarit (en-tête) :
// la liste des voyages, le voyage sélectionné et le chemin courant.
@ControllerAdvice(basePackages = "projet.saik.controller.web")
public class LayoutAdvice {

    private final TripService tripService;
    private final SelectedTripResolver resolver;

    public LayoutAdvice(TripService tripService, SelectedTripResolver resolver) {
        this.tripService = tripService;
        this.resolver = resolver;
    }

    @ModelAttribute
    public void layout(Model model, HttpSession session, Principal principal, HttpServletRequest request) {
        model.addAttribute("currentPath", request.getRequestURI());
        if (principal == null) {
            return;
        }
        List<TripResponse> trips;
        try {
            trips = tripService.getTrips();
        } catch (ResponseStatusException ex) {
            // Compte introuvable (ex. utilisateur de test) : la page s'affiche sans voyage.
            trips = List.of();
        }
        LocalDate today = LocalDate.now();
        TripResponse selected = resolver.resolve(trips, session);
        model.addAttribute("trips", TripView.of(trips, today));
        model.addAttribute("selectedTrip", selected == null ? null : TripView.of(selected, today));
        model.addAttribute("countryCatalog", List.of(CountryIso.values()));
        Map<String, EmergencyInfo> emergencyByIso = new LinkedHashMap<>();
        if (selected != null) {
            selected.countries().forEach(country -> CountryIso.fromCode(country.iso())
                    .map(EmergencyDirectory::find)
                    .ifPresent(info -> emergencyByIso.put(country.iso(), info)));
        }
        model.addAttribute("emergencyByIso", emergencyByIso);
    }
}
