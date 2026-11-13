package projet.saik.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Le nom du pays, ne peut pas être null
    @Column(nullable = false)
    private String name;

    //colonne codeIso est unique et ne peut pas être null, longueur maximale de 3 caractères
    @Column(nullable = false, unique = true, length = 3)
    private String codeIso;

    //Un pays peut être associé à plusieurs voyages, et un voyage peut inclure plusieurs pays
    @ManyToMany(mappedBy = "countries")
    private Set<Trip> trips = new HashSet<>();

    //Constructeur par défaut
    public Country() {}

    //Constructeur avec paramètres pour initialiser le nom et le code ISO du pays
    public Country(String name, String codeIso) {
        this.name = name;
        //Le code ISO est un code unique de 2 ou 3 lettres qui identifie le pays, par exemple "FR" pour la France ou "USA" pour les États-Unis. Il est utilisé pour normaliser les références aux pays dans les systèmes informatiques et les bases de données.
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