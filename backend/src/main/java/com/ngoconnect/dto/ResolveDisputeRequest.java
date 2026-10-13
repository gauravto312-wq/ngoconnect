package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResolveDisputeRequest {
    private String resolution; // e.g. "PAID", "RESOLVED", "REJECTED"
    private String remarks;
}
