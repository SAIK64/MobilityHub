package projet.saik.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
        name = "trips_countries",
        joinColumns = @JoinColumn(name = "trip_id"),
        inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    private Set<Country> countries = new HashSet<>();

    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private Budget budget;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransportTicket> transportTickets = new ArrayList<>();

    public Trip() {}

    @Transient
    public String getTimeRemaining() {
        if (startDate == null || endDate == null) return "";
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) {
            long days = ChronoUnit.DAYS.between(today, startDate);
            return "Débute dans " + days + " jour(s)";
        } else if (!today.isAfter(endDate)) {
            long days = ChronoUnit.DAYS.between(today, endDate);
            return "En cours — reste " + days + " jour(s)";
        } else {
            return "Voyage terminé";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Set<Country> getCountries() { return countries; }
    public void setCountries(Set<Country> countries) { this.countries = countries; }

    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) {
        this.budget = budget;
        if (budget != null && budget.getTrip() != this) {
            budget.setTrip(this);
        }
    }

    public List<TransportTicket> getTransportTickets() { return transportTickets; }
    public void setTransportTickets(List<TransportTicket> transportTickets) { this.transportTickets = transportTickets; }
}