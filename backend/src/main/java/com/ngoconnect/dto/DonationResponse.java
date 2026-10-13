package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DonationResponse {
    private Long id;
    private Long donorUserId;
    private String donorName;
    private String donorRole;
    private String donorContact;
    private Long restaurantBranchId;
    private String branchName;
    private String category;
    private String title;
    private String description;
    private Integer quantity;
    private String unit;
    private String address;
    private String city;
    private String state;
    private LocalDateTime availabilityStart;
    private LocalDateTime availabilityEnd;
    private String pickupCode; // Protected: Exposed only to authorized donor & claiming NGO
    private String status;
    private Long claimedByNgoId;
    private String claimedByNgoName;
    private LocalDateTime claimedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime completedAt;
    private Long assignedVolunteerId;
    private String assignedVolunteerName;
    private String assignedVolunteerContact;
    private String taskStatus;
    private LocalDateTime createdAt;
}
