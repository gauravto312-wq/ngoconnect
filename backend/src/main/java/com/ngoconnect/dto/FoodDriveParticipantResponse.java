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
public class FoodDriveParticipantResponse {
    private Long id;
    private Long foodDriveId;
    private Long userId;
    private String userName;
    private String participantType; // NGO, RESTAURANT, VOLUNTEER
    private String contributionDescription;
    private Integer contributionQuantity;
    private String contributionUnit;
    private String status;
    private LocalDateTime joinedAt;
}
