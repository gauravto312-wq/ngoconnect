package com.ngoconnect.repository;

import com.ngoconnect.entity.Volunteer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {
    Optional<Volunteer> findByUserId(Long userId);
    Optional<Volunteer> findByVolunteerCode(String volunteerCode);
    List<Volunteer> findByCityContainingIgnoreCaseAndAvailabilityStatus(String city, String availabilityStatus);
    List<Volunteer> findByAvailabilityStatus(String availabilityStatus);
    Long countByAvailabilityStatus(String availabilityStatus);
}
