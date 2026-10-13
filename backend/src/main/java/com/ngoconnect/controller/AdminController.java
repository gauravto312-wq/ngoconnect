package com.ngoconnect.controller;

import com.ngoconnect.dto.*;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.getOrDefault("isActive", true);
        return ResponseEntity.ok(adminService.toggleUserStatus(id, isActive));
    }

    @GetMapping("/verifications/ngo/pending")
    public ResponseEntity<List<NgoVerificationResponse>> getPendingNgoVerifications() {
        return ResponseEntity.ok(adminService.getPendingNgoVerifications());
    }

    @PostMapping("/verifications/ngo/{id}/review")
    public ResponseEntity<NgoVerificationResponse> reviewNgoVerification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> body) {
        String status = body.get("status"); // APPROVED, REJECTED
        String remarks = body.get("remarks");
        return ResponseEntity.ok(adminService.reviewNgoVerification(id, currentUser.getId(), status, remarks));
    }

    @GetMapping("/ngos")
    public ResponseEntity<List<NgoProfileResponse>> getAllNgos() {
        return ResponseEntity.ok(adminService.getAllNgos());
    }

    @PatchMapping("/ngos/{id}/status")
    public ResponseEntity<NgoProfileResponse> reviewNgoStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(adminService.reviewNgoStatus(id, currentUser.getId(), status));
    }

    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantProfileResponse>> getAllRestaurants() {
        return ResponseEntity.ok(adminService.getAllRestaurants());
    }

    @PatchMapping("/restaurants/{id}/status")
    public ResponseEntity<RestaurantProfileResponse> reviewRestaurantStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(adminService.reviewRestaurantStatus(id, currentUser.getId(), status));
    }
}
