package com.ngoconnect.repository;

import com.ngoconnect.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorUserId(Long donorUserId);
    List<Donation> findByStatus(String status);
    List<Donation> findByCityContainingIgnoreCaseAndStatus(String city, String status);
    List<Donation> findByCategoryAndStatus(String category, String status);
    Long countByStatus(String status);
}
