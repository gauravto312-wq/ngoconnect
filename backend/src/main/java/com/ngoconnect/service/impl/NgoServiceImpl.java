package com.ngoconnect.service.impl;

import com.ngoconnect.dto.NgoProfileResponse;
import com.ngoconnect.dto.NgoVerificationRequest;
import com.ngoconnect.dto.NgoVerificationResponse;
import com.ngoconnect.entity.Ngo;
import com.ngoconnect.entity.NgoVerification;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.NgoRepository;
import com.ngoconnect.repository.NgoVerificationRepository;
import com.ngoconnect.service.NgoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.ngoconnect.service.NotificationService;

@Service
@RequiredArgsConstructor
public class NgoServiceImpl implements NgoService {

    private final NgoRepository ngoRepository;
    private final NgoVerificationRepository ngoVerificationRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public NgoProfileResponse getNgoProfile(Long userId) {
        Ngo ngo = ngoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user id: " + userId));
        return mapToResponse(ngo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NgoProfileResponse> getPublicNgos(String search, String city) {
        List<Ngo> ngos;
        if ((search != null && !search.isBlank()) || (city != null && !city.isBlank())) {
            ngos = ngoRepository.searchNgos(search, city);
        } else {
            ngos = ngoRepository.findAll();
        }
        return ngos.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NgoProfileResponse getPublicNgoById(Long id) {
        Ngo ngo = ngoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NGO not found with id: " + id));
        return mapToResponse(ngo);
    }

    @Override
    @Transactional
    public NgoProfileResponse updatePhotos(Long userId, String profileImageUrl, String galleryPhotos) {
        Ngo ngo = ngoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user id: " + userId));

        if (profileImageUrl != null) {
            ngo.setProfileImageUrl(profileImageUrl);
        }
        if (galleryPhotos != null) {
            ngo.setGalleryPhotos(galleryPhotos);
        }

        ngo = ngoRepository.save(ngo);
        return mapToResponse(ngo);
    }

    @Override
    @Transactional
    public NgoVerificationResponse submitVerificationDocument(Long userId, NgoVerificationRequest request) {
        Ngo ngo = ngoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user id: " + userId));

        NgoVerification verif = NgoVerification.builder()
                .ngo(ngo)
                .documentType(request.getDocumentType())
                .documentUrl(request.getDocumentUrl())
                .documentNumber(request.getDocumentNumber())
                .verificationStatus("PENDING")
                .build();

        verif = ngoVerificationRepository.save(verif);

        return mapToVerifResponse(verif);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NgoVerificationResponse> getNgoVerificationDocuments(Long userId) {
        Ngo ngo = ngoRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found for user id: " + userId));

        return ngoVerificationRepository.findByNgoId(ngo.getId())
                .stream().map(this::mapToVerifResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendPublicInquiry(Long ngoId, String senderName, String senderContact, String messageText) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO not found with id: " + ngoId));

        if (ngo.getUser() != null) {
            String fullMessage = "From: " + senderName + " (" + senderContact + ")\nMessage: " + messageText;
            notificationService.sendNotification(
                    ngo.getUser().getId(),
                    "New Public Message / Inquiry from " + senderName,
                    fullMessage,
                    "DIRECT_INQUIRY",
                    "NGO",
                    ngo.getId()
            );
        }
    }

    private NgoProfileResponse mapToResponse(Ngo n) {
        String verifStatus = n.getVerificationStatus();
        if (verifStatus == null || verifStatus.isBlank()) {
            verifStatus = "PENDING";
        }
        return NgoProfileResponse.builder()
                .id(n.getId())
                .userId(n.getUser() != null ? n.getUser().getId() : null)
                .ngoName(n.getNgoName())
                .description(n.getDescription())
                .cause(n.getCause())
                .contactNumber(n.getContactNumber())
                .email(n.getUser() != null ? n.getUser().getEmail() : null)
                .phone(n.getUser() != null ? n.getUser().getPhone() : n.getContactNumber())
                .address(n.getAddress())
                .city(n.getCity())
                .state(n.getState())
                .profileImageUrl(n.getProfileImageUrl())
                .galleryPhotos(n.getGalleryPhotos())
                .verificationStatus(verifStatus)
                .createdAt(n.getCreatedAt())
                .build();
    }

    private NgoVerificationResponse mapToVerifResponse(NgoVerification v) {
        return NgoVerificationResponse.builder()
                .id(v.getId())
                .ngoId(v.getNgo().getId())
                .ngoName(v.getNgo().getNgoName())
                .documentType(v.getDocumentType())
                .documentUrl(v.getDocumentUrl())
                .documentNumber(v.getDocumentNumber())
                .verificationStatus(v.getVerificationStatus())
                .adminName(v.getAdmin() != null ? v.getAdmin().getName() : null)
                .remarks(v.getRemarks())
                .reviewedAt(v.getReviewedAt())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
