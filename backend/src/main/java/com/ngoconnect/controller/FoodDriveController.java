package com.ngoconnect.controller;

import com.ngoconnect.dto.*;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.FoodDriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/food-drives")
@RequiredArgsConstructor
public class FoodDriveController {

    private final FoodDriveService foodDriveService;

    @PostMapping
    public ResponseEntity<FoodDriveResponse> createFoodDrive(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody FoodDriveRequest request) {
        return new ResponseEntity<>(foodDriveService.createFoodDrive(currentUser.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/public")
    public ResponseEntity<List<FoodDriveResponse>> getPublicFoodDrives(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(foodDriveService.getFoodDrives(city, status, currentUserId));
    }

    @GetMapping
    public ResponseEntity<List<FoodDriveResponse>> getAllFoodDrives(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(foodDriveService.getFoodDrives(city, status, currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDriveResponse> getFoodDriveById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(foodDriveService.getFoodDriveById(id, currentUserId));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<FoodDriveParticipantResponse> joinOrContribute(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody FoodDriveParticipantRequest request) {
        return ResponseEntity.ok(foodDriveService.joinOrContribute(id, currentUser.getId(), request));
    }

    @GetMapping("/my-drives")
    public ResponseEntity<List<FoodDriveResponse>> getMyFoodDrives(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(foodDriveService.getFoodDrivesByHost(currentUser.getId()));
    }

    @PostMapping("/{id}/evidence-photos")
    public ResponseEntity<FoodDriveResponse> addEvidencePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody FoodDrivePhotoRequest request) {
        return ResponseEntity.ok(foodDriveService.addEvidencePhoto(id, currentUser.getId(), request));
    }

    @DeleteMapping("/{id}/evidence-photos")
    public ResponseEntity<FoodDriveResponse> removeEvidencePhoto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody FoodDrivePhotoRequest request) {
        return ResponseEntity.ok(foodDriveService.removeEvidencePhoto(id, currentUser.getId(), request));
    }

    @PostMapping(value = {"/{id}/evidence-photos/submit", "/{id}/photos/submit"})
    public ResponseEntity<FoodDriveResponse> submitEvidencePhotos(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody(required = false) FoodDrivePhotoRequest request) {
        return ResponseEntity.ok(foodDriveService.submitEvidencePhotos(id, currentUser.getId(), request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<FoodDriveResponse> completeFoodDrive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(foodDriveService.completeFoodDrive(id, currentUser.getId()));
    }
}
