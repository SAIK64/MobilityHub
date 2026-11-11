package projet.saik.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import projet.saik.model.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByUserIdOrderByStartDateAsc(Long userId);
    Optional<Trip> findByIdAndUserId(Long id, Long userId);
}