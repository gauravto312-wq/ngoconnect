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
public class NgoVerificationResponse {
    private Long id;
    private Long ngoId;
    private String ngoName;
    private String documentType;
    private String documentUrl;
    private String documentNumber;
    private String verificationStatus;
    private String adminName;
    private String remarks;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
