package projet.saik.controller.web;

import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import projet.saik.service.TripService;

@Controller
// Change le voyage sélectionné (mémorisé dans la session HTTP) depuis n'importe quelle page.
public class TripSelectionController {

    // Pages vers lesquelles on peut revenir après la sélection (évite les redirections arbitraires).
    private static final Set<String> RETURN_PAGES = Set.of("/dashboard", "/transports", "/budget", "/securite", "/profil");

    private final TripService tripService;

    public TripSelectionController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/trips/select")
    public String select(@RequestParam Long tripId,
            @RequestParam(defaultValue = "/dashboard") String returnTo,
            HttpSession session, RedirectAttributes flash) {
        // On ne mémorise que les voyages appartenant à la personne connectée.
        tripService.getTrips().stream()
                .filter(trip -> trip.id().equals(tripId))
                .findFirst()
                .ifPresentOrElse(trip -> {
                    session.setAttribute(SelectedTripResolver.SESSION_KEY, trip.id());
                    flash.addFlashAttribute("message", "Voyage sélectionné : " + trip.name() + ".");
                }, () -> flash.addFlashAttribute("error", "Voyage introuvable."));
        return "redirect:" + (RETURN_PAGES.contains(returnTo) ? returnTo : "/dashboard");
    }
}
