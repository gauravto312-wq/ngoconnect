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
public class FoodAvailabilityResponse {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private Long branchId;
    private String branchName;
    private String foodType;
    private String description;
    private Integer quantity;
    private String unit;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private LocalDateTime pickupDeadline;
    private String status; // AVAILABLE, CLAIMED, EXPIRED
    private LocalDateTime createdAt;
}
