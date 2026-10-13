package com.ngoconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FoodAvailabilityRequest {
    private Long branchId;

    @NotBlank(message = "Food type is required")
    private String foodType;

    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String unit = "meals";

    @NotNull(message = "Available from time is required")
    private LocalDateTime availableFrom;

    @NotNull(message = "Available until time is required")
    private LocalDateTime availableUntil;

    @NotNull(message = "Pickup deadline is required")
    private LocalDateTime pickupDeadline;
}
