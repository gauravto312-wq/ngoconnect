package com.ngoconnect.repository;

import com.ngoconnect.entity.RestaurantBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RestaurantBranchRepository extends JpaRepository<RestaurantBranch, Long> {
    List<RestaurantBranch> findByRestaurantId(Long restaurantId);
    List<RestaurantBranch> findByCityContainingIgnoreCase(String city);
}
