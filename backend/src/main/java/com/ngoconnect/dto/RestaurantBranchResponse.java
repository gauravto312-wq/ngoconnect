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
public class RestaurantBranchResponse {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private String branchName;
    private String address;
    private String city;
    private String state;
    private String contactNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
