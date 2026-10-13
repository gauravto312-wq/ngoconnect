package com.ngoconnect.repository;

import com.ngoconnect.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByUserId(Long userId);
    List<Restaurant> findByCityContainingIgnoreCase(String city);
    List<Restaurant> findByVerificationStatus(String status);

    @Query("SELECT r FROM Restaurant r WHERE " +
           "(:city IS NULL OR :city = '' OR LOWER(r.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(r.restaurantName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Restaurant> searchRestaurants(@Param("search") String search, @Param("city") String city);
}
