package com.ngoconnect.repository;

import com.ngoconnect.entity.FoodDriveParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FoodDriveParticipantRepository extends JpaRepository<FoodDriveParticipant, Long> {
    List<FoodDriveParticipant> findByFoodDriveId(Long foodDriveId);
    List<FoodDriveParticipant> findByUserId(Long userId);
    Optional<FoodDriveParticipant> findByFoodDriveIdAndUserId(Long foodDriveId, Long userId);
}
