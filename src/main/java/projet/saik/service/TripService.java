package projet.saik.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import projet.saik.model.Budget;
import projet.saik.model.Country;
import projet.saik.model.CountryIso;
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

    // Recherche parmi les voyages de la personne connectée (nom du voyage, nom ou code ISO d’un pays).
    @Transactional(readOnly = true)
    public List<TripResponse> searchTrips(String query) {
        // Une recherche vide revient à lister tous les voyages.
        if (query == null || query.isBlank()) {
            return getTrips();
        }
        User user = access.requireCurrentUser();
        return tripRepository.searchByUser(user.getId(), query.trim())
                .stream()
                .map(this::toTripResponse)
                .toList();
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {
        // Refuse une période incohérente avant toute écriture en base.
        validateDates(request);

        // Prépare une nouvelle entité de voyage, rattachée au compte authentifié.
        Trip trip = new Trip();
        trip.setUser(access.requireCurrentUser());
        // Copie le nom, les dates et les pays demandés.
        applyRequest(trip, request);

        // Chaque voyage possède un budget créé au même moment (relation 1-1).
        Budget budget = new Budget();
        budget.setTrip(trip);
        trip.setBudget(budget);

        // Enregistre le voyage, puis renvoie sa représentation destinée à l’interface.
        return toTripResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse updateTrip(Long tripId, TripRequest request) {
        // Refuse une période incohérente avant toute écriture en base.
        validateDates(request);

        // Vérifie que le voyage appartient bien à la personne connectée, puis le met à jour.
        Trip trip = access.requireOwnedTrip(tripId);
        applyRequest(trip, request);

        // Enregistre le voyage mis à jour et renvoie sa représentation destinée à l’interface.
        return toTripResponse(tripRepository.save(trip));
    }

    // Règle commune à la création et à la modification : la fin du séjour ne peut pas précéder son début.
    private void validateDates(TripRequest request) {
        if (request.end().isBefore(request.start())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit suivre la date de départ.");
        }
    }

    // Copie le nom, les dates et la liste des pays de la requête dans le voyage (création ou modification).
    private void applyRequest(Trip trip, TripRequest request) {
        // Accepte la liste « countries » ou, pour compatibilité avec l’API, un seul pays « country » / « iso ».
        List<CountryRequest> requestedCountries = request.countries() == null || request.countries().isEmpty()
                ? (request.country() == null || request.iso() == null
                        ? List.of()
                        : List.of(new CountryRequest(request.country(), request.iso())))
                : request.countries();
        if (requestedCountries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un pays est requis.");
        }

        // Nettoie le nom et enregistre les dates du séjour.
        trip.setName(request.name().trim());
        trip.setStartDate(request.start());
        trip.setEndDate(request.end());

        // Remplace les pays du voyage : réutilise les pays connus et ignore les codes ISO en double.
        trip.getCountries().clear();
        requestedCountries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        country -> normalizeIso(country.iso()),
                        country -> country,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                .forEach((iso, requestedCountry) -> trip.getCountries().add(getOrCreateCountry(requestedCountry, iso)));
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
        String iso = normalizeIso(request.iso());
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

    private String normalizeIso(String requestedIso) {
        return CountryIso.fromCode(requestedIso)
                .map(CountryIso::getCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Code ISO pays invalide ou non pris en charge : " + requestedIso));
    }

    private Country getOrCreateCountry(CountryRequest request, String iso) {
        CountryIso countryIso = CountryIso.fromCode(iso).orElseThrow();
        return countryRepository.findByCodeIso(iso).orElseGet(() -> countryRepository.save(
                new Country(countryIso.getDisplayName(), iso)));
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
