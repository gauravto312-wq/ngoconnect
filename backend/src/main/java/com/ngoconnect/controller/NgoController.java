package com.ngoconnect.controller;

import com.ngoconnect.dto.NgoProfileResponse;
import com.ngoconnect.dto.NgoVerificationRequest;
import com.ngoconnect.dto.NgoVerificationResponse;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.NgoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ngos")
@RequiredArgsConstructor
public class NgoController {

    private final NgoService ngoService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<NgoProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ngoService.getNgoProfile(currentUser.getId()));
    }

    @GetMapping("/public")
    public ResponseEntity<List<NgoProfileResponse>> getPublicNgos(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ngoService.getPublicNgos(search, city));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<NgoProfileResponse> getPublicNgoById(@PathVariable Long id) {
        return ResponseEntity.ok(ngoService.getPublicNgoById(id));
    }

    @PostMapping("/public/{id}/contact")
    public ResponseEntity<java.util.Map<String, String>> sendPublicInquiry(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        String senderName = body.getOrDefault("senderName", "Public Visitor");
        String senderContact = body.getOrDefault("senderContact", "N/A");
        String message = body.get("message");
        ngoService.sendPublicInquiry(id, senderName, senderContact, message);
        return ResponseEntity.ok(java.util.Map.of("message", "Inquiry sent successfully to NGO!"));
    }

    @PutMapping("/photos")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<NgoProfileResponse> updatePhotos(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody com.ngoconnect.dto.PhotoUpdateRequest request) {
        return ResponseEntity.ok(ngoService.updatePhotos(currentUser.getId(), request.getProfileImageUrl(), request.getGalleryPhotos()));
    }

    @PostMapping("/verification-documents")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<NgoVerificationResponse> submitVerification(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody NgoVerificationRequest request) {
        return new ResponseEntity<>(ngoService.submitVerificationDocument(currentUser.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/verification-documents")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<NgoVerificationResponse>> getVerificationDocuments(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(ngoService.getNgoVerificationDocuments(currentUser.getId()));
    }
}
