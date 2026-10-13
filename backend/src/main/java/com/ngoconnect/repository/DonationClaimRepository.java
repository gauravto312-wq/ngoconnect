package com.ngoconnect.repository;

import com.ngoconnect.entity.DonationClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DonationClaimRepository extends JpaRepository<DonationClaim, Long> {
    Optional<DonationClaim> findByDonationId(Long donationId);
    List<DonationClaim> findByNgoId(Long ngoId);
    Boolean existsByDonationId(Long donationId);
}
