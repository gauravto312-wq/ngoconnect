package com.ngoconnect.repository;

import com.ngoconnect.entity.NgoVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NgoVerificationRepository extends JpaRepository<NgoVerification, Long> {
    List<NgoVerification> findByNgoId(Long ngoId);
    List<NgoVerification> findByVerificationStatus(String status);
}
