package projet.saik.controller.web;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;
import projet.saik.web.TravelDataDtos.TripResponse;

@Component
public class SelectedTripResolver {

    // Clé sous laquelle l'id du voyage choisi est gardé dans la session HTTP.
    public static final String SESSION_KEY = "selectedTripId";

    // Renvoie le voyage choisi, ou le premier voyage de la liste, ou null s'il n'y en a aucun.
    public TripResponse resolve(List<TripResponse> trips, HttpSession session) {
        Object stored = session.getAttribute(SESSION_KEY);
        if (stored instanceof Long id) {
            for (TripResponse trip : trips) {
                if (trip.id().equals(id)) {
                    return trip;
                }
            }
        }
        return trips.isEmpty() ? null : trips.get(0);
    }
}