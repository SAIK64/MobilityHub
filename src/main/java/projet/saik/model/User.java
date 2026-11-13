package projet.saik.model;

import java.time.LocalDate;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "users")
// Entité représentant un compte utilisateur, ses rôles et ses voyages.
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // Adresse de connexion, qui doit être unique dans la base.
    @Column(nullable = false, unique = true)
    private String email;

    // Mot de passe conservé sous forme d’empreinte BCrypt.
    @Column(nullable = false)
    private String password;

    // Date de naissance renseignée dans le profil.
    private String birthDate;
    // Nationalité déclarée par la personne.
    private String nationality;
    // Numéro de téléphone de contact.
    private String phone;
    // Numéro de passeport, s’il est renseigné.
    private String passport;

    // Charge les rôles pour que Spring Security puisse établir les autorisations du compte.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "users_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Voyages appartenant à ce compte, supprimés en cascade avec lui.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    public User() {}

    // Calcule le nombre de pays visités à partir des voyages déjà terminés.
    @Transient
    public int getVisitedCountriesCount() {
        // Une liste de voyages absente signifie qu’aucun pays n’est comptabilisé.
        if (trips == null) return 0;
        // Ne retient que les voyages terminés avant aujourd’hui.
        LocalDate today = LocalDate.now();
        return (int) trips.stream()
                .filter(trip -> trip.getEndDate() != null && trip.getEndDate().isBefore(today))
                .flatMap(trip -> trip.getCountries().stream())
                .map(Country::getId)
                .distinct()
                .count();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Retourne la date de naissance du profil.
    public String getBirthDate() { return birthDate; }
    // Met à jour la date de naissance du profil.
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    // Retourne la nationalité renseignée.
    public String getNationality() { return nationality; }
    // Met à jour la nationalité renseignée.
    public void setNationality(String nationality) { this.nationality = nationality; }

    // Retourne le numéro de téléphone.
    public String getPhone() { return phone; }
    // Met à jour le numéro de téléphone.
    public void setPhone(String phone) { this.phone = phone; }

    // Retourne le numéro de passeport.
    public String getPassport() { return passport; }
    // Met à jour le numéro de passeport.
    public void setPassport(String passport) { this.passport = passport; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public List<Trip> getTrips() { return trips; }
    public void setTrips(List<Trip> trips) { this.trips = trips; }
}