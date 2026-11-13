package projet.saik.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.TransportTicket;

// Fournit les opérations de persistance sur les titres de transport.
public interface TransportTicketRepository extends JpaRepository<TransportTicket, Long> {
    // Trie les billets d’un voyage selon leur horaire de départ.
    List<TransportTicket> findByTripIdOrderByDepartureTimeAsc(Long tripId);
}