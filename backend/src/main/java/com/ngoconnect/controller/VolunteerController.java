package com.ngoconnect.controller;

import com.ngoconnect.dto.VolunteerProfileResponse;
import com.ngoconnect.dto.VolunteerStatusUpdateRequest;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<VolunteerProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(volunteerService.getVolunteerProfile(currentUser.getId()));
    }

    @PatchMapping("/status")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<VolunteerProfileResponse> updateStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody VolunteerStatusUpdateRequest request) {
        return ResponseEntity.ok(volunteerService.updateStatus(currentUser.getId(), request));
    }

    @GetMapping("/active-search")
    public ResponseEntity<List<VolunteerProfileResponse>> searchActiveVolunteers(
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(volunteerService.searchActiveVolunteers(city));
    }

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<com.ngoconnect.dto.VolunteerEarningsResponse> getEarnings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(volunteerService.getVolunteerEarnings(currentUser.getId()));
    }
}
