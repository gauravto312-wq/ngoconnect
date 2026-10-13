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

import com.ngoconnect.service.FileStorageService;
import com.ngoconnect.service.CloudinaryService;
import com.ngoconnect.service.NotificationService;
import com.ngoconnect.exception.BadRequestException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NgoServiceImpl implements NgoService {

    private final NgoRepository ngoRepository;
    private final NgoVerificationRepository ngoVerificationRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;

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

    @Override
    @Transactional(readOnly = true)
    public String getVerificationDocumentViewUrl(Long verificationId, Long currentUserId, boolean isAdmin) {
        NgoVerification verif = ngoVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification document not found with id: " + verificationId));

        validateVerificationAccess(verif, currentUserId, isAdmin);

        String docUrl = verif.getDocumentUrl();
        if (docUrl != null && docUrl.contains("cloudinary.com") && cloudinaryService.isConfigured()) {
            String signedUrl = cloudinaryService.generateSignedDocumentUrl(docUrl);
            if (signedUrl != null && !signedUrl.isBlank()) {
                return signedUrl;
            }
            log.warn("Expiring Cloudinary URL generation failed for doc id {}. Using backend streaming endpoint.", verificationId);
        }

        return "/api/v1/ngos/verification-documents/" + verificationId + "/file";
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> serveVerificationDocumentFile(Long verificationId, Long currentUserId, boolean isAdmin) {
        NgoVerification verif = ngoVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification document not found with id: " + verificationId));

        validateVerificationAccess(verif, currentUserId, isAdmin);

        String docUrl = verif.getDocumentUrl();
        if (docUrl == null || docUrl.isBlank()) {
            throw new ResourceNotFoundException("No document file associated with this record.");
        }

        // 1. Cloudinary hosted
        if (docUrl.contains("cloudinary.com")) {
            if (cloudinaryService.isConfigured()) {
                String signedUrl = cloudinaryService.generateSignedDocumentUrl(docUrl);
                if (signedUrl != null && !signedUrl.isBlank()) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(signedUrl))
                            .build();
                }
            }
            log.error("Unable to securely deliver Cloudinary document id {}: Cloudinary is unconfigured or signed URL generation failed", verificationId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Secure document storage service is currently unavailable."));
        }

        // 2. Local filesystem
        String filename = docUrl.substring(docUrl.lastIndexOf('/') + 1);
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Invalid document filename.");
        }

        Path privatePath = fileStorageService.getPrivateDocumentsDirectory().resolve(filename).normalize();
        Path publicPath = fileStorageService.getUploadDirectory().resolve(filename).normalize();

        Path resolvedFile = null;
        if (Files.exists(privatePath) && Files.isRegularFile(privatePath) && privatePath.startsWith(fileStorageService.getPrivateDocumentsDirectory())) {
            resolvedFile = privatePath;
        } else if (Files.exists(publicPath) && Files.isRegularFile(publicPath) && publicPath.startsWith(fileStorageService.getUploadDirectory())) {
            resolvedFile = publicPath;
        }

        if (resolvedFile == null) {
            throw new ResourceNotFoundException("Document file not found on disk: " + filename);
        }

        try {
            Resource resource = new UrlResource(resolvedFile.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Document file is unreadable: " + filename);
            }

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (lower.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (lower.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Error loading document file: " + filename);
        }
    }

    private void validateVerificationAccess(NgoVerification verif, Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (verif.getNgo() == null || verif.getNgo().getUser() == null ||
                !verif.getNgo().getUser().getId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You do not have permission to access this verification document.");
        }
    }
}
