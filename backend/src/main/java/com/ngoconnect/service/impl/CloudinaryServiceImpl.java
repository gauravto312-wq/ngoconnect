package com.ngoconnect.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ngoconnect.service.CloudinaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryServiceImpl(@Autowired(required = false) Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public boolean isConfigured() {
        return cloudinary != null;
    }

    @Override
    public String uploadImage(MultipartFile file, String subfolder) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured. Please supply CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty or null file.");
        }

        String targetFolder = "ngoconnect/" + (subfolder != null && !subfolder.isBlank() ? subfolder.trim() : "public");

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", targetFolder,
                "resource_type", "image",
                "overwrite", true
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

        String secureUrl = (String) uploadResult.get("secure_url");
        if (secureUrl == null || secureUrl.isBlank()) {
            throw new IOException("Cloudinary did not return a secure URL for the uploaded file.");
        }

        log.info("Public media uploaded successfully to Cloudinary: public_id={}", uploadResult.get("public_id"));
        return secureUrl;
    }

    @Override
    public String uploadPrivateDocument(MultipartFile file, String subfolder) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary is not configured. Please supply CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty or null file.");
        }

        String targetFolder = "ngoconnect/private/" + (subfolder != null && !subfolder.isBlank() ? subfolder.trim() : "documents");

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", targetFolder,
                "resource_type", "auto",
                "type", "authenticated",
                "overwrite", true
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

        String secureUrl = (String) uploadResult.get("secure_url");
        if (secureUrl == null || secureUrl.isBlank()) {
            throw new IOException("Cloudinary did not return a secure URL for the uploaded document.");
        }

        log.info("Private document uploaded securely to Cloudinary: public_id={}", uploadResult.get("public_id"));
        return secureUrl;
    }

    @Override
    public String generateSignedDocumentUrl(String rawUrlOrPublicId) {
        return generateSignedDocumentUrl(rawUrlOrPublicId, 300);
    }

    @Override
    public String generateSignedDocumentUrl(String rawUrlOrPublicId, int expirationSeconds) {
        if (!isConfigured() || rawUrlOrPublicId == null || rawUrlOrPublicId.isBlank()) {
            return null;
        }

        if (expirationSeconds <= 0) {
            expirationSeconds = 300;
        }

        String cleanUrl = rawUrlOrPublicId.trim();

        // Strip any existing query parameters (e.g. untrusted or expired expires_at, signature, etc.)
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex != -1) {
            cleanUrl = cleanUrl.substring(0, queryIndex);
        }

        String resourceType = "image";
        String publicId;
        String format = null;

        if (cleanUrl.contains("cloudinary.com")) {
            // Strictly require /authenticated/ delivery path; reject public /upload/ assets
            int markerIndex = cleanUrl.indexOf("/authenticated/");
            if (markerIndex == -1) {
                log.error("Rejecting document URL: Cloudinary URL is not an authenticated asset (missing /authenticated/).");
                return null;
            }

            // Extract resource_type (e.g. image, raw)
            String prefix = cleanUrl.substring(0, markerIndex);
            int lastSlash = prefix.lastIndexOf('/');
            if (lastSlash != -1) {
                String parsedType = prefix.substring(lastSlash + 1);
                if ("image".equalsIgnoreCase(parsedType) || "raw".equalsIgnoreCase(parsedType) || "video".equalsIgnoreCase(parsedType)) {
                    resourceType = parsedType.toLowerCase();
                }
            }

            String rest = cleanUrl.substring(markerIndex + "/authenticated/".length());
            // Strip any static signature component s--...--/
            if (rest.matches("^s--[^/]+--/.*")) {
                rest = rest.substring(rest.indexOf('/') + 1);
            }
            // Strip version v12345.../
            if (rest.matches("^v[0-9]+/.*")) {
                rest = rest.substring(rest.indexOf('/') + 1);
            }

            if (rest.isBlank()) {
                log.error("Rejecting document URL: Cloudinary URL has empty asset identifier.");
                return null;
            }
            publicId = rest;
        } else {
            publicId = cleanUrl;
        }

        if (publicId.contains("..") || publicId.startsWith("/") || publicId.startsWith("\\")) {
            log.error("Rejecting document public ID: Path traversal detected.");
            return null;
        }

        int lastDot = publicId.lastIndexOf('.');
        int lastSlash = publicId.lastIndexOf('/');
        if (lastDot > 0 && lastDot > lastSlash) {
            format = publicId.substring(lastDot + 1);
            publicId = publicId.substring(0, lastDot);
        }

        long expiresAt = (System.currentTimeMillis() / 1000L) + expirationSeconds;
        Map<String, Object> options = ObjectUtils.asMap(
                "resource_type", resourceType,
                "type", "authenticated",
                "expires_at", expiresAt,
                "attachment", false
        );

        try {
            String signedUrl = cloudinary.privateDownload(publicId, format, options);
            if (signedUrl == null || signedUrl.isBlank()) {
                log.error("Cloudinary privateDownload returned empty URL.");
                return null;
            }
            return signedUrl;
        } catch (Exception e) {
            log.error("Failed to generate secure expiring Cloudinary download URL: provider exception");
            return null;
        }
    }
}
