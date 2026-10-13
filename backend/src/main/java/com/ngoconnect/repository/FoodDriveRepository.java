package com.ngoconnect.repository;

import com.ngoconnect.entity.FoodDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodDriveRepository extends JpaRepository<FoodDrive, Long> {
    List<FoodDrive> findByCreatedByUserId(Long userId);
    List<FoodDrive> findByCityContainingIgnoreCase(String city);
    List<FoodDrive> findByStatus(String status);
}
