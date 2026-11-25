package projet.saik.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
// Entité représentant un rôle de sécurité attribuable à des comptes.
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom unique du rôle utilisé par les règles de sécurité.
    @Column(nullable = false, unique = true)
    private String name;

    // Liste inverse des comptes auxquels ce rôle est attribué.
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    // Charge les permissions du rôle pour les contrôles d’accès.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "roles_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }

    public Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
}