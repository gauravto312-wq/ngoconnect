package com.ngoconnect.controller;

import com.ngoconnect.dto.*;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<RestaurantProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(restaurantService.getRestaurantProfile(currentUser.getId()));
    }

    @GetMapping("/public")
    public ResponseEntity<List<RestaurantProfileResponse>> getPublicRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(restaurantService.getPublicRestaurants(search, city));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<RestaurantProfileResponse> getPublicRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getPublicRestaurantById(id));
    }

    @PostMapping("/public/{id}/contact")
    public ResponseEntity<java.util.Map<String, String>> sendPublicInquiry(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String senderName = body.getOrDefault("senderName", "Public Visitor");
        String senderContact = body.getOrDefault("senderContact", "N/A");
        String message = body.get("message");
        restaurantService.sendPublicInquiry(id, senderName, senderContact, message);
        return ResponseEntity.ok(java.util.Map.of("message", "Inquiry sent successfully to Restaurant!"));
    }

    @PutMapping("/photos")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<RestaurantProfileResponse> updatePhotos(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody PhotoUpdateRequest request) {
        return ResponseEntity.ok(restaurantService.updatePhotos(currentUser.getId(), request.getProfileImageUrl(), request.getGalleryPhotos()));
    }

    @PostMapping("/branches")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<RestaurantBranchResponse> addBranch(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody RestaurantBranchRequest request) {
        return new ResponseEntity<>(restaurantService.addBranch(currentUser.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/branches")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<List<RestaurantBranchResponse>> getBranches(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(restaurantService.getBranches(currentUser.getId()));
    }

    @PostMapping("/food-availability")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<FoodAvailabilityResponse> postFoodAvailability(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody FoodAvailabilityRequest request) {
        return new ResponseEntity<>(restaurantService.postFoodAvailability(currentUser.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/food-availability")
    @PreAuthorize("hasRole('RESTAURANT')")
    public ResponseEntity<List<FoodAvailabilityResponse>> getFoodAvailabilities(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(restaurantService.getFoodAvailabilities(currentUser.getId()));
    }
}
