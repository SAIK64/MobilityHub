package projet.saik.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import projet.saik.model.Budget;
import projet.saik.model.Expense;
import projet.saik.model.FlightDetails;
import projet.saik.model.TransportTicket;
import projet.saik.model.Trip;
import projet.saik.repository.ExpenseRepository;
import projet.saik.repository.TransportTicketRepository;
import projet.saik.web.TravelDataDtos.FlightDetailsRequest;
import projet.saik.web.TravelDataDtos.FlightDetailsResponse;
import projet.saik.web.TravelDataDtos.TicketRequest;
import projet.saik.web.TravelDataDtos.TicketResponse;

@Service
// Règles métier des titres de transport : un billet payant génère automatiquement une dépense « Transport ».
public class TicketService {

    // Accès aux titres de transport enregistrés.
    private final TransportTicketRepository ticketRepository;
    // Accès aux dépenses générées par les billets.
    private final ExpenseRepository expenseRepository;
    // Contrôles d’identité et de propriété.
    private final AccessService access;

    public TicketService(TransportTicketRepository ticketRepository, ExpenseRepository expenseRepository, AccessService access) {
        this.ticketRepository = ticketRepository;
        this.expenseRepository = expenseRepository;
        this.access = access;
    }

    // Consulte les titres après vérification de la propriété du voyage.
    @Transactional(readOnly = true)
    public List<TicketResponse> getTickets(Long tripId) {
        // Refuse l’accès si le voyage n’existe pas ou appartient à quelqu’un d’autre.
        Trip trip = access.requireOwnedTrip(tripId);
        // Trie les titres par départ et convertit chaque entité en réponse JSON.
        return ticketRepository.findByTripIdOrderByDepartureTimeAsc(trip.getId())
                .stream()
                .map(this::toTicketResponse)
                .toList();
    }

    @Transactional
    public TicketResponse createTicket(Long tripId, TicketRequest request) {
        // Vérifie l’accès au voyage avant de lui ajouter des données.
        Trip trip = access.requireOwnedTrip(tripId);

        // Prépare le titre, le relie au voyage (relation 1-N) et copie les informations saisies.
        TransportTicket ticket = new TransportTicket();
        ticket.setTrip(trip);
        applyRequest(ticket, request);
        // Un prix strictement positif crée aussi une dépense « Transport » dans le budget.
        syncGeneratedExpense(ticket);

        // Enregistre le titre et renvoie ses données à l’interface.
        return toTicketResponse(ticketRepository.save(ticket));
    }

    // Modifie un titre existant après vérification de la propriété du voyage.
    @Transactional
    public TicketResponse updateTicket(Long ticketId, TicketRequest request) {
        TransportTicket ticket = requireOwnedTicket(ticketId);
        applyRequest(ticket, request);
        // Garde la dépense générée cohérente avec le nouveau prix (création, mise à jour ou suppression).
        syncGeneratedExpense(ticket);
        return toTicketResponse(ticketRepository.save(ticket));
    }

    // Supprime un titre et la dépense automatique qu’il aurait créée.
    @Transactional
    public void deleteTicket(Long ticketId) {
        TransportTicket ticket = requireOwnedTicket(ticketId);
        // Garde une référence à la dépense avant de supprimer le titre.
        Expense generatedExpense = ticket.getGeneratedExpense();
        // Supprime le titre de la base.
        ticketRepository.delete(ticket);
        // Si le titre avait généré une dépense, retire-la aussi du budget et de la base.
        if (generatedExpense != null) {
            generatedExpense.getBudget().getExpenses().remove(generatedExpense);
            expenseRepository.delete(generatedExpense);
        }
    }

    // Charge un titre seulement s’il appartient à un voyage de la personne connectée (sinon 404).
    private TransportTicket requireOwnedTicket(Long ticketId) {
        TransportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Titre introuvable."));
        access.requireOwnedTrip(ticket.getTrip().getId());
        return ticket;
    }

    // Copie les informations saisies dans le titre (commun à la création et à la modification).
    private void applyRequest(TransportTicket ticket, TicketRequest request) {
        // Type et description sans espaces inutiles.
        ticket.setTransportType(request.type().trim());
        ticket.setManualDescription(request.description().trim());
        // Lieux, horaires, prix, référence et nom du justificatif.
        ticket.setDeparture(request.departure());
        ticket.setArrival(request.arrival());
        ticket.setDepartureTime(request.departureTime());
        ticket.setArrivalTime(request.arrivalTime());
        ticket.setPrice(request.price());
        ticket.setApiBookingReference(request.reference());
        ticket.setAttachmentName(request.attachmentName());
        // Un vol porte des détails supplémentaires et des lieux construits à partir des aéroports.
        if ("Avion".equalsIgnoreCase(request.type().trim())) {
            FlightDetails flightDetails = toFlightDetails(request);
            ticket.setFlightDetails(flightDetails);
            ticket.setDeparture(flightDetails.getDepartureAirport() + " (" + flightDetails.getDepartureIata() + ")");
            ticket.setArrival(flightDetails.getArrivalAirport() + " (" + flightDetails.getArrivalIata() + ")");
            // La compagnie aérienne est dans les détails du vol : pas d’opérateur terrestre.
            ticket.setOperatorName(null);
            ticket.setServiceNumber(null);
            ticket.setSeatInfo(null);
        } else {
            ticket.setFlightDetails(null);
            // Train, VTC/taxi, bus, ferry : opérateur, numéro de service et place.
            ticket.setOperatorName(trimToNull(request.operatorName()));
            ticket.setServiceNumber(trimToNull(request.serviceNumber()));
            ticket.setSeatInfo(trimToNull(request.seatInfo()));
        }
    }

    // Règle métier : un titre payant a toujours une dépense « Transport » à son montant dans le budget du voyage.
    // - prix > 0 et pas encore de dépense : on la crée ;
    // - prix > 0 et dépense existante : on la met à jour ;
    // - prix = 0 et dépense existante : on la supprime.
    private void syncGeneratedExpense(TransportTicket ticket) {
        Expense expense = ticket.getGeneratedExpense();
        boolean paid = ticket.getPrice() != null && ticket.getPrice() > 0;

        if (!paid) {
            if (expense != null) {
                // Détache puis supprime la dépense devenue inutile.
                ticket.setGeneratedExpense(null);
                expense.getBudget().getExpenses().remove(expense);
                expenseRepository.delete(expense);
            }
            return;
        }

        if (expense == null) {
            // Prépare une dépense automatique rattachée au budget du voyage (créé si besoin).
            Budget budget = access.requireBudget(ticket.getTrip());
            expense = new Expense();
            expense.setCategory("Transport");
            expense.setIsAutoGenerated(true);
            expense.setBudget(budget);
            budget.getExpenses().add(expense);
        }
        // Reprend la description, le montant et la date de départ du titre.
        expense.setDescription(ticket.getManualDescription());
        expense.setAmount(ticket.getPrice());
        expense.setExpenseDate(ticket.getDepartureTime().toLocalDate());
        ticket.setGeneratedExpense(expenseRepository.save(expense));
    }

    // Convertit un titre JPA vers le format stable attendu par l’interface.
    private TicketResponse toTicketResponse(TransportTicket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTransportType(),
                ticket.getManualDescription(),
                ticket.getDeparture(),
                ticket.getArrival(),
                ticket.getDepartureTime(),
                ticket.getArrivalTime(),
                ticket.getPrice(),
                ticket.getApiBookingReference(),
                ticket.getAttachmentName(),
                toFlightDetailsResponse(ticket.getFlightDetails()),
                ticket.getOperatorName(),
                ticket.getServiceNumber(),
                ticket.getSeatInfo());
    }

    private FlightDetails toFlightDetails(TicketRequest request) {
        FlightDetailsRequest details = request.flightDetails();
        if (details == null
                || isBlank(details.departureAirport()) || isBlank(details.arrivalAirport())
                || !matches(details.departureIata(), "[A-Za-z]{3}")
                || !matches(details.arrivalIata(), "[A-Za-z]{3}")
                || isBlank(details.airline()) || isBlank(details.fareType())
                || !matches(details.eticketNumber(), "\\d{13}")
                || !matches(request.reference(), "[A-Za-z0-9]{6}")
                || details.stopCount() == null || details.stopCount() < 0 || details.stopCount() > 20
                || !("Confirmé / OK".equals(details.ticketStatus())
                        || "En attente".equals(details.ticketStatus())
                        || "Annulé".equals(details.ticketStatus()))
                || details.departureUtcOffsetMinutes() == null || details.arrivalUtcOffsetMinutes() == null
                || !isValidUtcOffset(details.departureUtcOffsetMinutes())
                || !isValidUtcOffset(details.arrivalUtcOffsetMinutes())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pour un vol, renseignez les aéroports, les codes IATA, la compagnie, le tarif, "
                            + "un numéro de billet à 13 chiffres, un PNR à 6 caractères et les fuseaux horaires.");
        }
        if (request.arrivalTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L’heure locale d’arrivée du vol est obligatoire.");
        }
        if (details.stopCount() > 0 && isBlank(details.stopDetails())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Précisez les aéroports et les durées des escales.");
        }

        long durationMinutes = Duration.between(
                request.departureTime().minusMinutes(details.departureUtcOffsetMinutes()),
                request.arrivalTime().minusMinutes(details.arrivalUtcOffsetMinutes())).toMinutes();
        if (durationMinutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La durée du vol doit être positive ; vérifiez les horaires locaux et leurs décalages UTC.");
        }

        FlightDetails flight = new FlightDetails();
        flight.setDepartureAirport(details.departureAirport().trim());
        flight.setDepartureIata(details.departureIata().trim().toUpperCase());
        flight.setArrivalAirport(details.arrivalAirport().trim());
        flight.setArrivalIata(details.arrivalIata().trim().toUpperCase());
        flight.setStopCount(details.stopCount());
        flight.setStopDetails(trimToNull(details.stopDetails()));
        flight.setAirline(details.airline().trim());
        flight.setFlightNumber(trimToNull(details.flightNumber()));
        flight.setFareType(details.fareType().trim());
        flight.setCabinBaggage(trimToNull(details.cabinBaggage()));
        flight.setCabinBaggageWeightKg(details.cabinBaggageWeightKg());
        flight.setCabinBaggageDimensions(trimToNull(details.cabinBaggageDimensions()));
        flight.setPersonalItem(trimToNull(details.personalItem()));
        flight.setPersonalItemDetails(trimToNull(details.personalItemDetails()));
        flight.setCheckedBaggage(trimToNull(details.checkedBaggage()));
        flight.setCheckedBaggageWeightKg(details.checkedBaggageWeightKg());
        flight.setCheckedBaggageDimensions(trimToNull(details.checkedBaggageDimensions()));
        flight.setModificationConditions(trimToNull(details.modificationConditions()));
        flight.setCancellationConditions(trimToNull(details.cancellationConditions()));
        flight.setSeatChoice(trimToNull(details.seatChoice()));
        flight.setExtraBaggage(trimToNull(details.extraBaggage()));
        flight.setSpecialMeal(trimToNull(details.specialMeal()));
        flight.setOptionalServices(trimToNull(details.optionalServices()));
        flight.setEticketNumber(details.eticketNumber().trim());
        flight.setTicketStatus(trimToNull(details.ticketStatus()));
        flight.setDepartureUtcOffsetMinutes(details.departureUtcOffsetMinutes());
        flight.setArrivalUtcOffsetMinutes(details.arrivalUtcOffsetMinutes());
        flight.setDurationMinutes(durationMinutes);
        return flight;
    }

    private FlightDetailsResponse toFlightDetailsResponse(FlightDetails flight) {
        if (flight == null) return null;
        return new FlightDetailsResponse(
                flight.getDepartureAirport(), flight.getDepartureIata(),
                flight.getArrivalAirport(), flight.getArrivalIata(),
                flight.getStopCount(), flight.getStopDetails(), flight.getAirline(),
                flight.getFlightNumber(), flight.getFareType(), flight.getCabinBaggage(),
                flight.getCabinBaggageWeightKg(), flight.getCabinBaggageDimensions(),
                flight.getPersonalItem(), flight.getPersonalItemDetails(), flight.getCheckedBaggage(),
                flight.getCheckedBaggageWeightKg(), flight.getCheckedBaggageDimensions(),
                flight.getModificationConditions(), flight.getCancellationConditions(),
                flight.getSeatChoice(), flight.getExtraBaggage(), flight.getSpecialMeal(),
                flight.getOptionalServices(), flight.getEticketNumber(), flight.getTicketStatus(),
                flight.getDepartureUtcOffsetMinutes(), flight.getArrivalUtcOffsetMinutes(),
                flight.getDurationMinutes());
    }

    private boolean isValidUtcOffset(int offsetMinutes) {
        return offsetMinutes >= -720 && offsetMinutes <= 840 && offsetMinutes % 15 == 0;
    }

    private boolean matches(String value, String pattern) {
        return value != null && value.matches(pattern);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
