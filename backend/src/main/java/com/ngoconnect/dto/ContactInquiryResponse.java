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
public class ContactInquiryResponse {
    private Long id;
    private String name;
    private String email;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}
