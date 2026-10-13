package com.ngoconnect.repository;

import com.ngoconnect.entity.FoodAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodAvailabilityRepository extends JpaRepository<FoodAvailability, Long> {
    List<FoodAvailability> findByRestaurantId(Long restaurantId);
    List<FoodAvailability> findByStatus(String status);
}
