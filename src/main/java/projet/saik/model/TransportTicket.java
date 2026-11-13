package projet.saik.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_tickets")
// Entité qui conserve un titre de transport associé à un voyage.
public class TransportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Type de transport : train, avion, taxi, etc.
    @Column(nullable = false)
    private String transportType;

    // Référence de réservation éventuellement fournie par un service externe.
    private String apiBookingReference;
    private String manualDescription;
    // Lieu d’où part le trajet.
    private String departure;
    // Lieu où se termine le trajet.
    private String arrival;
    private LocalDateTime departureTime;
    // Horaire d’arrivée, qui peut rester absent.
    private LocalDateTime arrivalTime;
    private Double price;
    // Nom du justificatif choisi, sans enregistrer le fichier lui-même.
    private String attachmentName;

    // Voyage auquel appartient le titre.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    // Dépense créée automatiquement à partir du prix de ce titre, s’il y en a une.
    @OneToOne
    @JoinColumn(name = "expense_id", unique = true)
    private Expense generatedExpense;

    public TransportTicket() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransportType() { return transportType; }
    public void setTransportType(String transportType) { this.transportType = transportType; }

    public String getApiBookingReference() { return apiBookingReference; }
    public void setApiBookingReference(String apiBookingReference) { this.apiBookingReference = apiBookingReference; }

    public String getManualDescription() { return manualDescription; }
    public void setManualDescription(String manualDescription) { this.manualDescription = manualDescription; }

    // Retourne le lieu de départ du trajet.
    public String getDeparture() { return departure; }
    // Modifie le lieu de départ du trajet.
    public void setDeparture(String departure) { this.departure = departure; }

    // Retourne le lieu d’arrivée du trajet.
    public String getArrival() { return arrival; }
    // Modifie le lieu d’arrivée du trajet.
    public void setArrival(String arrival) { this.arrival = arrival; }

    // Retourne la date et l’heure de départ.
    public LocalDateTime getDepartureTime() { return departureTime; }
    // Modifie la date et l’heure de départ.
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    // Retourne la date et l’heure d’arrivée si elles sont connues.
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    // Modifie la date et l’heure d’arrivée.
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    // Retourne le prix du titre.
    public Double getPrice() { return price; }
    // Modifie le prix du titre.
    public void setPrice(Double price) { this.price = price; }

    // Retourne le nom du justificatif choisi.
    public String getAttachmentName() { return attachmentName; }
    // Enregistre le nom du justificatif, sans stocker son contenu.
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Expense getGeneratedExpense() { return generatedExpense; }
    public void setGeneratedExpense(Expense generatedExpense) { this.generatedExpense = generatedExpense; }
}