package projet.saik.web;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import projet.saik.web.TravelDataDtos.CountryResponse;
import projet.saik.web.TravelDataDtos.TripResponse;

// Modèle de vue d'un voyage : données du voyage + informations calculées pour l'affichage
// (statut, durée, compte à rebours). Construit par les contrôleurs puis lu par les templates Thymeleaf.
@Getter
public class TripView {

    public static final String ACTIVE = "Actif";
    public static final String FUTURE = "Futur";
    public static final String ARCHIVED = "Archivé";

    private final Long id;
    private final String name;
    private final LocalDate start;
    private final LocalDate end;
    private final List<CountryResponse> countries;
    // Actif, Futur ou Archivé : sert aux filtres du tableau de bord.
    private final String status;
    private final String statusLabel;
    private final String statusIcon;
    private final String statusClasses;
    private final long durationDays;
    private final String countdownLabel;
    private final String countdownValue;
    private final String countdownUnit;
    private final String countdownExplainer;

    private TripView(TripResponse trip, LocalDate today) {
        this.id = trip.id();
        this.name = trip.name();
        this.start = trip.start();
        this.end = trip.end();
        this.countries = trip.countries() != null && !trip.countries().isEmpty()
                ? trip.countries()
                : List.of(new CountryResponse(trip.country(), trip.iso()));
        this.durationDays = ChronoUnit.DAYS.between(start, end) + 1;

        long daysUntilStart = ChronoUnit.DAYS.between(today, start);
        long daysUntilEnd = ChronoUnit.DAYS.between(today, end);
        if (end.isBefore(today)) {
            long daysSinceEnd = -daysUntilEnd;
            this.status = ARCHIVED;
            this.statusLabel = "Séjour terminé";
            this.statusIcon = "history";
            this.statusClasses = "bg-surface-container-high text-on-surface-variant";
            this.countdownLabel = "Voyage terminé depuis";
            this.countdownValue = String.valueOf(daysSinceEnd);
            this.countdownUnit = dayLabel(daysSinceEnd);
            this.countdownExplainer = "Les dates de fin du séjour sont passées.";
        } else if (start.isAfter(today)) {
            this.status = FUTURE;
            this.statusLabel = "À venir";
            this.statusIcon = "schedule";
            this.statusClasses = "bg-secondary-container text-on-secondary-container";
            this.countdownLabel = "Départ dans";
            this.countdownValue = String.valueOf(daysUntilStart);
            this.countdownUnit = dayLabel(daysUntilStart);
            this.countdownExplainer = "Le compte à rebours est calculé à partir de la date de départ.";
        } else {
            this.status = ACTIVE;
            this.statusLabel = "Séjour en cours";
            this.statusIcon = "flight_takeoff";
            this.statusClasses = "bg-primary-container text-on-primary-container";
            if (daysUntilEnd == 0) {
                this.countdownLabel = "Dernier jour du séjour";
                this.countdownValue = "Aujourd’hui";
                this.countdownUnit = "";
                this.countdownExplainer = "Le séjour se termine aujourd’hui.";
            } else {
                this.countdownLabel = "Fin du séjour dans";
                this.countdownValue = String.valueOf(daysUntilEnd);
                this.countdownUnit = dayLabel(daysUntilEnd);
                this.countdownExplainer = "Profitez bien de votre voyage !";
            }
        }
    }

    public static TripView of(TripResponse trip, LocalDate today) {
        return new TripView(trip, today);
    }

    public static List<TripView> of(List<TripResponse> trips, LocalDate today) {
        return trips.stream().map(trip -> of(trip, today)).toList();
    }

    // « FR, DE » : codes ISO de tous les pays du voyage.
    public String getIsoCodes() {
        return countries.stream().map(CountryResponse::iso).collect(Collectors.joining(", "));
    }

    // « France, Allemagne » : noms de tous les pays du voyage.
    public String getCountryNames() {
        return countries.stream().map(CountryResponse::country).collect(Collectors.joining(", "));
    }

    private static String dayLabel(long days) {
        return days == 1 ? "jour" : "jours";
    }
}
