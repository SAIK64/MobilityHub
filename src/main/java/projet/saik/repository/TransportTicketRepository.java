package projet.saik.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.TransportTicket;

public interface TransportTicketRepository extends JpaRepository<TransportTicket, Long> {
    List<TransportTicket> findByTripIdOrderByDepartureTimeAsc(Long tripId);
}