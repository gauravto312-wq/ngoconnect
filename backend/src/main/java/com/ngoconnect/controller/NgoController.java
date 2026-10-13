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

import com.ngoconnect.config.UploadRateLimiter;
import com.ngoconnect.service.CloudinaryService;
import com.ngoconnect.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ngos")
@RequiredArgsConstructor
@Slf4j
public class NgoController {

    private final NgoService ngoService;
    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    private final UploadRateLimiter rateLimiter;

    @Value("${app.upload.allow-local-fallback:false}")
    private boolean allowLocalFallback;

    private static final long MAX_DOC_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_DOC_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".pdf");
    private static final List<String> ALLOWED_DOC_MIMES = Arrays.asList("image/jpeg", "image/png", "image/webp", "application/pdf");

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

    @PostMapping("/verification-documents/upload")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<?> uploadVerificationDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required to upload verification documents."));
        }

        String rateLimitKey = "ngo_doc_" + currentUser.getId();
        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Upload quota exceeded. Maximum 20 uploads per minute allowed. Please try again later."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Verification document file is empty or missing."));
        }

        if (file.getSize() > MAX_DOC_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Verification document file size exceeds maximum limit of 5MB."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOC_MIMES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file type. Only PDF and image files (JPG, PNG, WEBP) are allowed."));
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_DOC_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file extension. Allowed extensions: PDF, JPG, JPEG, PNG, WEBP."));
        }

        try {
            if (cloudinaryService.isConfigured()) {
                String secureUrl = cloudinaryService.uploadPrivateDocument(file, "documents");
                return ResponseEntity.ok(Map.of("url", secureUrl));
            }

            if (!allowLocalFallback) {
                log.error("Cloudinary is not configured and local storage fallback is disabled in this environment.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "Persistent media storage is not configured. File uploads are temporarily unavailable."));
            }

            // Local private directory fallback when Cloudinary is not configured
            Path targetDir = fileStorageService.getPrivateDocumentsDirectory();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String newFilename = "doc_" + UUID.randomUUID().toString() + extension;
            Path destinationPath = targetDir.resolve(newFilename).normalize().toAbsolutePath();

            if (!destinationPath.startsWith(targetDir)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid file destination path: path traversal detected."));
            }

            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/api/v1/ngos/verification-documents/private/" + newFilename;
            return ResponseEntity.ok(Map.of("url", fileUrl));

        } catch (IOException e) {
            log.error("Failed to upload verification document safely: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload verification document safely."));
        }
    }

    @GetMapping("/verification-documents/{id}/view-url")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    public ResponseEntity<Map<String, String>> getDocumentViewUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        String viewUrl = ngoService.getVerificationDocumentViewUrl(id, currentUser.getId(), isAdmin);
        return ResponseEntity.ok(Map.of("url", viewUrl));
    }

    @GetMapping("/verification-documents/{id}/file")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    public ResponseEntity<?> getDocumentFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return ngoService.serveVerificationDocumentFile(id, currentUser.getId(), isAdmin);
    }
}
