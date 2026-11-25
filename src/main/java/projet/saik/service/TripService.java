package projet.saik.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import projet.saik.model.Budget;
import projet.saik.model.Country;
import projet.saik.model.Trip;
import projet.saik.model.User;
import projet.saik.repository.CountryRepository;
import projet.saik.repository.TripRepository;
import projet.saik.web.TravelDataDtos.CountryRequest;
import projet.saik.web.TravelDataDtos.CountryResponse;
import projet.saik.web.TravelDataDtos.TripRequest;
import projet.saik.web.TravelDataDtos.TripResponse;

@Service
// Règles métier des voyages et de leurs pays (relation N-N Trip <-> Country).
public class TripService {

    // Accès aux voyages enregistrés.
    private final TripRepository tripRepository;
    // Accès au catalogue des pays.
    private final CountryRepository countryRepository;
    // Contrôles d’identité et de propriété.
    private final AccessService access;

    public TripService(TripRepository tripRepository, CountryRepository countryRepository, AccessService access) {
        this.tripRepository = tripRepository;
        this.countryRepository = countryRepository;
        this.access = access;
    }

    // La lecture seule évite de modifier accidentellement les données en listant les voyages.
    @Transactional(readOnly = true)
    public List<TripResponse> getTrips() {
        // Récupère l’identité vérifiée de la personne connectée.
        User user = access.requireCurrentUser();
        // Ne charge que ses voyages, les trie par date de départ, puis les convertit en réponses.
        return tripRepository.findByUserIdOrderByStartDateAsc(user.getId())
                .stream()
                .map(this::toTripResponse)
                .toList();
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {
        // Refuse une période incohérente avant toute écriture en base.
        if (request.end().isBefore(request.start())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit suivre la date de départ.");
        }

        // Attache le nouveau voyage au compte authentifié.
        User user = access.requireCurrentUser();
        List<CountryRequest> requestedCountries = request.countries() == null || request.countries().isEmpty()
                ? (request.country() == null || request.iso() == null
                        ? List.of()
                        : List.of(new CountryRequest(request.country(), request.iso())))
                : request.countries();
        if (requestedCountries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un pays est requis.");
        }

        // Prépare une nouvelle entité de voyage.
        Trip trip = new Trip();
        // Nettoie le nom fourni avant de l’enregistrer.
        trip.setName(request.name().trim());
        // Enregistre le premier jour du séjour.
        trip.setStartDate(request.start());
        // Enregistre le dernier jour du séjour.
        trip.setEndDate(request.end());
        // Lie le voyage à son propriétaire.
        trip.setUser(user);
        // Réutilise les pays connus et évite les doublons dans les destinations du voyage.
        requestedCountries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        country -> country.iso().trim().toUpperCase(),
                        country -> country,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .forEach((iso, requestedCountry) -> trip.getCountries().add(getOrCreateCountry(requestedCountry, iso)));

        // Chaque voyage possède un budget créé au même moment.
        Budget budget = new Budget();
        // Relie le budget au voyage.
        budget.setTrip(trip);
        // Maintient aussi la relation dans l’objet voyage.
        trip.setBudget(budget);

        // Enregistre le voyage, puis renvoie sa représentation destinée à l’API.
        return toTripResponse(tripRepository.save(trip));
    }

    @Transactional 
    public TripResponse updateTrip(Long tripId, TripRequest request) {
        // Refuse une période incohérente avant toute écriture en base.
        if (request.end().isBefore(request.start())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit suivre la date de départ.");
        }

        // Vérifie que le voyage appartient bien à la personne connectée.
        Trip trip = access.requireOwnedTrip(tripId);
        // Nettoie le nom fourni avant de l’enregistrer.
        trip.setName(request.name().trim());
        // Met à jour les dates du séjour.
        trip.setStartDate(request.start());
        trip.setEndDate(request.end());

        // Réutilise les pays connus et évite les doublons dans les destinations du voyage.
        List<CountryRequest> requestedCountries = request.countries() == null || request.countries().isEmpty()
                ? (request.country() == null || request.iso() == null
                        ? List.of()
                        : List.of(new CountryRequest(request.country(), request.iso())))
                : request.countries();
        if (requestedCountries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un pays est requis.");
        }
        trip.getCountries().clear();
        requestedCountries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        country -> country.iso().trim().toUpperCase(),
                        country -> country,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .forEach((iso, requestedCountry) -> trip.getCountries().add(getOrCreateCountry(requestedCountry, iso)));

        // Enregistre le voyage mis à jour et renvoie sa représentation destinée à l’API.
        return toTripResponse(tripRepository.save(trip));
    }

    @Transactional 
    public TripResponse deleteTrip(Long tripId) {
        // Vérifie que le voyage appartient bien à la personne connectée.
        Trip trip = access.requireOwnedTrip(tripId);
        // Supprime le voyage et toutes les données qui lui sont liées.
        tripRepository.delete(trip);
        // Renvoie les informations du voyage supprimé pour confirmation.
        return toTripResponse(trip);
    }

    @Transactional
    public TripResponse addCountry(Long tripId, CountryRequest request) {
        Trip trip = access.requireOwnedTrip(tripId);
        String iso = request.iso().trim().toUpperCase();
        // TO DO 
        // mettre le iso en enum pour éviter les erreurs de saisie
        //
        Country country = getOrCreateCountry(request, iso);
        boolean alreadyAssociated = trip.getCountries().stream()
                .anyMatch(existing -> existing.getCodeIso().equalsIgnoreCase(iso));
        if (!alreadyAssociated) trip.getCountries().add(country);
        return toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse removeCountry(Long tripId, String requestedIso) {
        Trip trip = access.requireOwnedTrip(tripId);
        String iso = requestedIso.trim().toUpperCase();
        boolean removed = trip.getCountries().removeIf(country -> country.getCodeIso().equalsIgnoreCase(iso));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce pays n’est pas associé à ce voyage.");
        }
        return toTripResponse(tripRepository.save(trip));
    }

    private Country getOrCreateCountry(CountryRequest request, String iso) {
        return countryRepository.findByCodeIso(iso).orElseGet(() -> countryRepository.save(
                new Country(request.country().trim(), iso)));
    }

    // Convertit l’entité de voyage en réponse JSON sans exposer l’entité JPA directement.
    private TripResponse toTripResponse(Trip trip) {
        List<CountryResponse> countries = trip.getCountries().stream()
                .sorted(java.util.Comparator.comparing(Country::getName))
                .map(country -> new CountryResponse(country.getName(), country.getCodeIso()))
                .toList();
        CountryResponse country = countries.isEmpty() ? null : countries.get(0);
        return new TripResponse(
                trip.getId(),
                trip.getName(),
                country == null ? "" : country.country(),
                country == null ? "" : country.iso(),
                countries,
                trip.getStartDate(),
                trip.getEndDate());
    }
}
