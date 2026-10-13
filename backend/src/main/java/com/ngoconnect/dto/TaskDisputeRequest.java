package com.ngoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDisputeRequest {
    @NotBlank(message = "Dispute reason is required")
    private String reason;
}
