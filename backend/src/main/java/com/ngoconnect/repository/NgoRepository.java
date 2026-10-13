package com.ngoconnect.repository;

import com.ngoconnect.entity.Ngo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NgoRepository extends JpaRepository<Ngo, Long> {
    Optional<Ngo> findByUserId(Long userId);
    List<Ngo> findByCityContainingIgnoreCase(String city);
    List<Ngo> findByVerificationStatus(String status);

    @Query("SELECT n FROM Ngo n WHERE " +
           "(:city IS NULL OR :city = '' OR LOWER(n.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(n.ngoName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.city) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.cause) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Ngo> searchNgos(@Param("search") String search, @Param("city") String city);
}
