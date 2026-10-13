package com.ngoconnect.repository;

import com.ngoconnect.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByCityContainingIgnoreCase(String city);
}
