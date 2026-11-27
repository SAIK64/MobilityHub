package projet.saik.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;
import projet.saik.web.TravelDataDtos.FlightDetailsRequest;
import projet.saik.web.TravelDataDtos.FlightDetailsResponse;
import projet.saik.web.TravelDataDtos.TicketRequest;
import projet.saik.web.TravelDataDtos.TicketResponse;

// Objet de formulaire (« form backing bean ») du billet de transport.
// Spring MVC le remplit à partir des champs du formulaire Thymeleaf (th:object / th:field),
// puis le contrôleur le convertit en TicketRequest pour le service. Sert à la création ET à la modification.
@Getter
@Setter
public class TicketForm {

    // Choix proposés par les listes déroulantes du formulaire de vol : {valeur enregistrée, libellé affiché}.
    public static final Map<String, List<List<String>>> CHOICES = Map.of(
            "fareType", List.of(List.of("", "Sélectionner un tarif"), List.of("Economy Light", "Economy Light"),
                    List.of("Economy Standard", "Economy Standard"), List.of("Economy Flex", "Economy Flex"),
                    List.of("Premium Economy", "Premium Economy"), List.of("Business", "Business"),
                    List.of("First", "First"), List.of("Autre", "Autre")),
            "ticketStatus", List.of(List.of("Confirmé / OK", "Confirmé / OK"), List.of("En attente", "En attente"),
                    List.of("Annulé", "Annulé")),
            "cabinBaggage", List.of(List.of("À vérifier", "À confirmer selon la compagnie et le tarif"),
                    List.of("Aucun", "Aucun"), List.of("Accessoire sous le siège", "Accessoire personnel uniquement"),
                    List.of("Cabine - repère 56 × 45 × 25 cm", "Bagage cabine (repère IATA : 56 × 45 × 25 cm)"),
                    List.of("Cabine + accessoire", "Bagage cabine + accessoire personnel")),
            "personalItem", List.of(List.of("À vérifier", "À confirmer selon la compagnie et le tarif"),
                    List.of("Oui", "Oui"), List.of("Non", "Non")),
            "checkedBaggage", List.of(List.of("À vérifier", "À confirmer selon la compagnie et le tarif"),
                    List.of("Aucun", "Aucun"), List.of("1 pièce", "1 pièce"), List.of("2 pièces", "2 pièces"),
                    List.of("Autre", "Autre")),
            "conditions", List.of(List.of("À vérifier", "À vérifier"), List.of("Remboursable", "Remboursable"),
                    List.of("Modifiable sans frais", "Modifiable sans frais"),
                    List.of("Modifiable avec frais", "Modifiable avec frais"),
                    List.of("Non remboursable", "Non remboursable")),
            "seatChoice", List.of(List.of("À choisir", "À choisir"), List.of("Siège standard", "Siège standard"),
                    List.of("Espace jambes supplémentaire", "Espace jambes supplémentaire"),
                    List.of("Siège premium", "Siège premium"), List.of("Non sélectionné", "Non sélectionné")),
            "specialMeal", List.of(List.of("Aucun", "Aucun"), List.of("Végétarien", "Végétarien"),
                    List.of("Végétalien", "Végétalien"), List.of("Sans gluten", "Sans gluten"),
                    List.of("Halal", "Halal"), List.of("Casher", "Casher"), List.of("Autre", "Autre")));

    // Format envoyé par les champs <input type="datetime-local">.
    private static final String DATETIME_LOCAL = "yyyy-MM-dd'T'HH:mm";

    private String type = "Avion";
    private String description;
    private String departure;
    private String arrival;
    @DateTimeFormat(pattern = DATETIME_LOCAL)
    private LocalDateTime departureTime;
    @DateTimeFormat(pattern = DATETIME_LOCAL)
    private LocalDateTime arrivalTime;
    private Double price;
    private String reference;
    private String attachmentName;
    // Train, VTC/taxi, bus, ferry.
    private String operatorName;
    private String serviceNumber;
    private String seatInfo;
    // Avion : champs « flight.xxx » du formulaire.
    private FlightForm flight = new FlightForm();

    // Vrai pour un billet d’avion (nommé ainsi pour ne pas entrer en conflit avec getFlight()).
    public boolean isAirTravel() {
        return "Avion".equalsIgnoreCase(type);
    }

    // Formulaire -> requête du service.
    public TicketRequest toRequest() {
        if (isAirTravel()) {
            return new TicketRequest(type, description, null, null, departureTime, arrivalTime, price,
                    upper(reference), attachmentName, flight.toRequest(), null, null, null);
        }
        return new TicketRequest(type, description, departure, arrival, departureTime, arrivalTime, price,
                reference, attachmentName, null, operatorName, serviceNumber, seatInfo);
    }

    // Billet enregistré -> formulaire pré-rempli (modification).
    public static TicketForm from(TicketResponse ticket) {
        TicketForm form = new TicketForm();
        form.type = ticket.type();
        form.description = ticket.description();
        form.departure = ticket.departure();
        form.arrival = ticket.arrival();
        form.departureTime = ticket.departureTime();
        form.arrivalTime = ticket.arrivalTime();
        form.price = ticket.price();
        form.reference = ticket.reference();
        form.attachmentName = ticket.attachmentName();
        form.operatorName = ticket.operatorName();
        form.serviceNumber = ticket.serviceNumber();
        form.seatInfo = ticket.seatInfo();
        if (ticket.flightDetails() != null) {
            form.flight = FlightForm.from(ticket.flightDetails());
        }
        return form;
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    // Partie « vol » du formulaire.
    @Getter
    @Setter
    public static class FlightForm {
        private String departureAirport;
        private String departureIata;
        private String arrivalAirport;
        private String arrivalIata;
        private Integer stopCount = 0;
        private String stopDetails;
        private String airline;
        private String flightNumber;
        private String fareType;
        private String ticketStatus = "Confirmé / OK";
        private Integer departureUtcOffsetMinutes;
        private Integer arrivalUtcOffsetMinutes;
        private String cabinBaggage;
        private Double cabinBaggageWeightKg;
        private String cabinBaggageDimensions;
        private String personalItem;
        private String personalItemDetails;
        private String checkedBaggage;
        private Double checkedBaggageWeightKg;
        private String checkedBaggageDimensions;
        private String modificationConditions;
        private String cancellationConditions;
        private String seatChoice;
        private String specialMeal;
        private String extraBaggage;
        private String optionalServices;
        private String eticketNumber;

        FlightDetailsRequest toRequest() {
            return new FlightDetailsRequest(departureAirport, upper(departureIata), arrivalAirport, upper(arrivalIata),
                    stopCount, stopDetails, airline, flightNumber, fareType,
                    cabinBaggage, cabinBaggageWeightKg, cabinBaggageDimensions,
                    personalItem, personalItemDetails,
                    checkedBaggage, checkedBaggageWeightKg, checkedBaggageDimensions,
                    modificationConditions, cancellationConditions, seatChoice,
                    extraBaggage, specialMeal, optionalServices, eticketNumber, ticketStatus,
                    departureUtcOffsetMinutes, arrivalUtcOffsetMinutes);
        }

        static FlightForm from(FlightDetailsResponse f) {
            FlightForm form = new FlightForm();
            form.departureAirport = f.departureAirport();
            form.departureIata = f.departureIata();
            form.arrivalAirport = f.arrivalAirport();
            form.arrivalIata = f.arrivalIata();
            form.stopCount = f.stopCount();
            form.stopDetails = f.stopDetails();
            form.airline = f.airline();
            form.flightNumber = f.flightNumber();
            form.fareType = f.fareType();
            form.ticketStatus = f.ticketStatus();
            form.departureUtcOffsetMinutes = f.departureUtcOffsetMinutes();
            form.arrivalUtcOffsetMinutes = f.arrivalUtcOffsetMinutes();
            form.cabinBaggage = f.cabinBaggage();
            form.cabinBaggageWeightKg = f.cabinBaggageWeightKg();
            form.cabinBaggageDimensions = f.cabinBaggageDimensions();
            form.personalItem = f.personalItem();
            form.personalItemDetails = f.personalItemDetails();
            form.checkedBaggage = f.checkedBaggage();
            form.checkedBaggageWeightKg = f.checkedBaggageWeightKg();
            form.checkedBaggageDimensions = f.checkedBaggageDimensions();
            form.modificationConditions = f.modificationConditions();
            form.cancellationConditions = f.cancellationConditions();
            form.seatChoice = f.seatChoice();
            form.specialMeal = f.specialMeal();
            form.extraBaggage = f.extraBaggage();
            form.optionalServices = f.optionalServices();
            form.eticketNumber = f.eticketNumber();
            return form;
        }
    }
}
