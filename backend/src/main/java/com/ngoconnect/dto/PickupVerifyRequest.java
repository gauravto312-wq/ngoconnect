package com.ngoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PickupVerifyRequest {
    @NotBlank(message = "Pickup code is required")
    private String pickupCode;
}
