package projet.saik.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_tickets")
public class TransportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transportType;

    private String apiBookingReference;
    private String manualDescription;
    private LocalDateTime departureTime;
    private Double price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

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

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Expense getGeneratedExpense() { return generatedExpense; }
    public void setGeneratedExpense(Expense generatedExpense) { this.generatedExpense = generatedExpense; }
}