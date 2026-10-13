package com.ngoconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NgoVerificationRequest {
    @NotBlank(message = "Document type is required")
    private String documentType;

    @NotBlank(message = "Document URL or path is required")
    private String documentUrl;

    @NotBlank(message = "Document number is required")
    private String documentNumber;
}
