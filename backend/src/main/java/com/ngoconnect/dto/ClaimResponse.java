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
public class ClaimResponse {
    private Long id;
    private Long donationId;
    private String donationTitle;
    private Long ngoId;
    private String ngoName;
    private String pickupCode;
    private String status;
    private LocalDateTime claimedAt;
}
