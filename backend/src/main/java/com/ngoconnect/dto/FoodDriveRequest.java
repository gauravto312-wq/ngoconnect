package com.ngoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FoodDriveRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String purpose;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotNull(message = "Event date is required")
    private LocalDate eventDate;

    @NotBlank(message = "Start time is required")
    private String startTime;

    @NotBlank(message = "End time is required")
    private String endTime;

    private java.time.LocalDateTime completionDeadline;
    private String coverImageUrl;
}
