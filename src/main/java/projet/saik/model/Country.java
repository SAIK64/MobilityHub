package projet.saik.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String codeIso;

    @ManyToMany(mappedBy = "countries")
    private Set<Trip> trips = new HashSet<>();

    public Country() {}

    public Country(String name, String codeIso) {
        this.name = name;
        this.codeIso = codeIso;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCodeIso() { return codeIso; }
    public void setCodeIso(String codeIso) { this.codeIso = codeIso; }

    public Set<Trip> getTrips() { return trips; }
    public void setTrips(Set<Trip> trips) { this.trips = trips; }
}