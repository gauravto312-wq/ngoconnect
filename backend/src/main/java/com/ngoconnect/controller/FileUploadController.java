package com.ngoconnect.controller;

import com.ngoconnect.config.UploadRateLimiter;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.CloudinaryService;
import com.ngoconnect.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    private final UploadRateLimiter rateLimiter;

    @Value("${app.upload.allow-local-fallback:false}")
    private boolean allowLocalFallback;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Object principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required to upload files."));
        }

        String rateLimitKey;
        if (principal instanceof UserPrincipal userPrincipal) {
            rateLimitKey = "user_" + userPrincipal.getId();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            rateLimitKey = "user_" + userDetails.getUsername();
        } else {
            rateLimitKey = "user_" + principal;
        }

        if (!rateLimiter.tryAcquire(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Upload quota exceeded. Maximum 20 uploads per minute allowed. Please try again later."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty or missing"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of("message", "File size exceeds maximum allowed limit of 5MB"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file type. Only image files (JPG, PNG, WEBP) are allowed."));
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file extension. Allowed extensions: JPG, JPEG, PNG, WEBP"));
        }

        try {
            if (cloudinaryService.isConfigured()) {
                String secureUrl = cloudinaryService.uploadImage(file, "public");
                return ResponseEntity.ok(Map.of("url", secureUrl));
            }

            if (!allowLocalFallback) {
                log.error("Cloudinary is not configured and local storage fallback is disabled in this environment.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "Persistent media storage is not configured. File uploads are temporarily unavailable."));
            }

            // Local disk fallback when Cloudinary is not configured (e.g. offline testing)
            Path targetDir = fileStorageService.getUploadDirectory();
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String newFilename = UUID.randomUUID().toString() + extension;
            Path destinationPath = targetDir.resolve(newFilename).normalize().toAbsolutePath();

            if (!destinationPath.startsWith(targetDir)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid file destination path: path traversal detected"));
            }

            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + newFilename;
            return ResponseEntity.ok(Map.of("url", fileUrl));

        } catch (IOException e) {
            log.error("Failed to upload file safely: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload file safely"));
        }
    }
}
