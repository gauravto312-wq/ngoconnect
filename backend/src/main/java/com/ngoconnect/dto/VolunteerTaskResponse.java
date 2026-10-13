package com.ngoconnect.dto;

import com.ngoconnect.entity.VolunteerTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VolunteerTaskResponse {
    private Long id;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerCode;
    private Long assignedByUserId;
    private String assignedByName;
    private String assignedByRole;
    private Long donationId;
    private Long foodDriveId;
    private String taskType;
    private String title;
    private String description;
    private String pickupAddress;
    private String destinationAddress;
    private LocalDateTime scheduledTime;
    private String status; // ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, REJECTED
    private Boolean isPaid;
    private BigDecimal paymentAmount;
    private String currency;
    private String paymentTerms;
    private String paymentStatus; // NOT_APPLICABLE, PENDING, PAID, DISPUTED
    private LocalDateTime paidAt;
    private Long paidByUserId;
    private LocalDateTime disputedAt;
    private String disputeReason;
    private LocalDateTime completedAt;
    private String pickupCode;
    private LocalDateTime createdAt;

    /**
     * Canonical response mapping from VolunteerTask entity to VolunteerTaskResponse DTO.
     * Uses explicit bean property setters on the zero-arg constructor to guarantee binary
     * compatibility and eliminate fragile multi-argument constructor mismatches.
     */
    public static VolunteerTaskResponse fromEntity(VolunteerTask t) {
        if (t == null) return null;
        VolunteerTaskResponse resp = new VolunteerTaskResponse();
        resp.setId(t.getId());
        if (t.getVolunteer() != null) {
            resp.setVolunteerId(t.getVolunteer().getId());
            if (t.getVolunteer().getUser() != null) {
                resp.setVolunteerName(t.getVolunteer().getUser().getName());
            }
            resp.setVolunteerCode(t.getVolunteer().getVolunteerCode());
        }
        if (t.getAssignedByUser() != null) {
            resp.setAssignedByUserId(t.getAssignedByUser().getId());
            resp.setAssignedByName(t.getAssignedByUser().getName());
            if (t.getAssignedByUser().getRole() != null) {
                resp.setAssignedByRole(t.getAssignedByUser().getRole().getName());
            }
        }
        resp.setDonationId(t.getDonation() != null ? t.getDonation().getId() : null);
        if (t.getDonation() != null) {
            resp.setPickupCode(t.getDonation().getPickupCode());
        }
        resp.setFoodDriveId(t.getFoodDrive() != null ? t.getFoodDrive().getId() : null);
        resp.setTaskType(t.getTaskType());
        resp.setTitle(t.getTitle());
        resp.setDescription(t.getDescription());
        resp.setPickupAddress(t.getPickupAddress());
        resp.setDestinationAddress(t.getDestinationAddress());
        resp.setScheduledTime(t.getScheduledTime());
        resp.setStatus(t.getStatus());
        resp.setIsPaid(Boolean.TRUE.equals(t.getIsPaid()));
        resp.setPaymentAmount(t.getPaymentAmount());
        resp.setCurrency(t.getCurrency() != null ? t.getCurrency() : "INR");
        resp.setPaymentTerms(t.getPaymentTerms());
        resp.setPaymentStatus(t.getPaymentStatus() != null ? t.getPaymentStatus() : "NOT_APPLICABLE");
        resp.setPaidAt(t.getPaidAt());
        resp.setPaidByUserId(t.getPaidByUserId());
        resp.setDisputedAt(t.getDisputedAt());
        resp.setDisputeReason(t.getDisputeReason());
        resp.setCompletedAt(t.getCompletedAt());
        resp.setCreatedAt(t.getCreatedAt());
        return resp;
    }
}
