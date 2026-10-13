package com.ngoconnect.controller;

import com.ngoconnect.dto.ContactInquiryRequest;
import com.ngoconnect.dto.ContactInquiryResponse;
import com.ngoconnect.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactInquiryResponse> submitInquiry(@Valid @RequestBody ContactInquiryRequest request) {
        return new ResponseEntity<>(contactService.submitInquiry(request), HttpStatus.CREATED);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContactInquiryResponse>> getAllInquiries() {
        return ResponseEntity.ok(contactService.getAllInquiries());
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactInquiryResponse> getInquiryById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getInquiryById(id));
    }
}
