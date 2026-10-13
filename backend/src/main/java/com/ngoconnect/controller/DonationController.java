package com.ngoconnect.controller;

import com.ngoconnect.dto.ClaimResponse;
import com.ngoconnect.dto.DonationRequest;
import com.ngoconnect.dto.DonationResponse;
import com.ngoconnect.dto.PickupVerifyRequest;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    public ResponseEntity<DonationResponse> createDonation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody DonationRequest request) {
        return new ResponseEntity<>(donationService.createDonation(currentUser.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/marketplace")
    public ResponseEntity<List<DonationResponse>> getMarketplace(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(donationService.getMarketplaceDonations(city, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationResponse> getDonationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(donationService.getDonationById(id, userId));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<ClaimResponse> claimDonation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(donationService.claimDonation(id, currentUser.getId()));
    }

    @GetMapping("/my-donations")
    public ResponseEntity<List<DonationResponse>> getMyDonations(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(donationService.getDonationsByDonor(currentUser.getId()));
    }

    @GetMapping("/claimed")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<DonationResponse>> getClaimedDonations(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(donationService.getClaimedDonationsByNgo(currentUser.getId()));
    }

    @PostMapping("/{id}/confirm-handover")
    public ResponseEntity<DonationResponse> confirmDonorHandover(
            @PathVariable Long id,
            @Valid @RequestBody PickupVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(donationService.confirmDonorHandover(id, request.getPickupCode(), currentUser.getId()));
    }

    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<DonationResponse> confirmNgoReceipt(
            @PathVariable Long id,
            @Valid @RequestBody PickupVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(donationService.confirmNgoReceipt(id, request.getPickupCode(), currentUser.getId()));
    }

    @PostMapping("/{id}/verify-pickup")
    public ResponseEntity<DonationResponse> verifyPickupCode(
            @PathVariable Long id,
            @Valid @RequestBody PickupVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(donationService.verifyPickupCode(id, request.getPickupCode(), currentUser.getId()));
    }
}
