package com.ngoconnect.repository;

import com.ngoconnect.entity.DonationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationStatusHistoryRepository extends JpaRepository<DonationStatusHistory, Long> {
    List<DonationStatusHistory> findByDonationIdOrderByCreatedAtAsc(Long donationId);
}
