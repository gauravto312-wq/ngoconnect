package com.ngoconnect.repository;

import com.ngoconnect.entity.VolunteerTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VolunteerTaskRepository extends JpaRepository<VolunteerTask, Long> {
    List<VolunteerTask> findByVolunteerId(Long volunteerId);
    List<VolunteerTask> findByVolunteerIdAndStatus(Long volunteerId, String status);
    List<VolunteerTask> findByVolunteerIdAndIsPaid(Long volunteerId, Boolean isPaid);
    List<VolunteerTask> findByAssignedByUserId(Long userId);
    List<VolunteerTask> findByDonationId(Long donationId);
    java.util.Optional<VolunteerTask> findFirstByDonationIdOrderByIdDesc(Long donationId);
    List<VolunteerTask> findByPaymentStatusOrderByUpdatedAtDesc(String paymentStatus);
}
