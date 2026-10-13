package com.ngoconnect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DonationRequest {
    @NotBlank(message = "Category is required")
    private String category; // FOOD, CLOTHES, OTHER

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String unit = "packets";

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    private Long restaurantBranchId;
    private LocalDateTime availabilityStart;
    private LocalDateTime availabilityEnd;
}
