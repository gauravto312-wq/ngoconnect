package com.ngoconnect.dto;

import lombok.Data;

@Data
public class FoodDriveParticipantRequest {
    private String contributionDescription;
    private Integer contributionQuantity = 0;
    private String contributionUnit = "meals";
}
