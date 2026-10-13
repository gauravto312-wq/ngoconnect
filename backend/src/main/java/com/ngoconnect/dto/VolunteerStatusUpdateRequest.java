package com.ngoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VolunteerStatusUpdateRequest {
    @NotBlank(message = "Availability status is required")
    private String availabilityStatus; // ACTIVE, INACTIVE
}
