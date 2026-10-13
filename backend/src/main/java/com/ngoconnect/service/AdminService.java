package com.ngoconnect.service;

import com.ngoconnect.dto.*;
import java.util.List;

public interface AdminService {
    AdminDashboardStatsResponse getDashboardStats();
    List<UserResponse> getAllUsers();
    UserResponse toggleUserStatus(Long userId, Boolean isActive);
    List<NgoVerificationResponse> getPendingNgoVerifications();
    NgoVerificationResponse reviewNgoVerification(Long verificationId, Long adminId, String status, String remarks);
    List<NgoProfileResponse> getAllNgos();
    NgoProfileResponse reviewNgoStatus(Long ngoId, Long adminId, String status);
    List<RestaurantProfileResponse> getAllRestaurants();
    RestaurantProfileResponse reviewRestaurantStatus(Long restaurantId, Long adminId, String status);
}
