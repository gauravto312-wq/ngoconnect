package com.ngoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VolunteerTaskRequest {
    @NotNull(message = "Volunteer ID is required")
    private Long volunteerId;

    private Long donationId;
    private Long foodDriveId;

    @NotBlank(message = "Task type is required")
    private String taskType; // PICKUP_DONATION, FOOD_DRIVE_HELP

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;

    private Boolean isPaid;
    private BigDecimal paymentAmount;
    private String currency; // default "INR"
    private String paymentTerms;
}
