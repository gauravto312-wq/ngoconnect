package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminDashboardStatsResponse {
    private Long totalUsers;
    private Long totalCitizens;
    private Long totalVolunteers;
    private Long activeVolunteers;
    private Long totalNgos;
    private Long pendingNgoVerifications;
    private Long totalRestaurants;
    private Long pendingRestaurantVerifications;
    private Long totalDonations;
    private Long availableDonations;
    private Long claimedDonations;
    private Long completedDonations;
    private Long totalFoodDrives;
}
