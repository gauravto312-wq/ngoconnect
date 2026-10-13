package com.ngoconnect.service.impl;

import com.ngoconnect.dto.ContactInquiryRequest;
import com.ngoconnect.dto.ContactInquiryResponse;
import com.ngoconnect.entity.ContactInquiry;
import com.ngoconnect.entity.User;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.ContactInquiryRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.service.ContactService;
import com.ngoconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactInquiryRepository contactInquiryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ContactInquiryResponse submitInquiry(ContactInquiryRequest request) {
        ContactInquiry inquiry = ContactInquiry.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .message(request.getMessage().trim())
                .status("NEW")
                .build();

        inquiry = contactInquiryRepository.save(inquiry);

        // Find all admin users to dispatch notification
        List<User> adminUsers = userRepository.findByRoleName("ADMIN");
        if (adminUsers.isEmpty()) {
            adminUsers = userRepository.findByRoleName("ROLE_ADMIN");
        }

        String notificationTitle = "New Contact Query from " + inquiry.getName();
        String notificationBody = String.format(
                "Name: %s\nEmail: %s\n\nMessage:\n%s\n\nSubmitted At: %s",
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getMessage(),
                inquiry.getCreatedAt() != null ? inquiry.getCreatedAt().toString() : "N/A"
        );

        for (User admin : adminUsers) {
            notificationService.sendNotification(
                    admin.getId(),
                    notificationTitle,
                    notificationBody,
                    "CONTACT_INQUIRY",
                    "CONTACT",
                    inquiry.getId()
            );
        }

        return mapToResponse(inquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactInquiryResponse> getAllInquiries() {
        return contactInquiryRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ContactInquiryResponse getInquiryById(Long id) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact inquiry not found with id: " + id));

        if ("NEW".equals(inquiry.getStatus())) {
            inquiry.setStatus("READ");
            inquiry = contactInquiryRepository.save(inquiry);
        }

        return mapToResponse(inquiry);
    }

    private ContactInquiryResponse mapToResponse(ContactInquiry inquiry) {
        return ContactInquiryResponse.builder()
                .id(inquiry.getId())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .message(inquiry.getMessage())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
