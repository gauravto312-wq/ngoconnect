package com.ngoconnect.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudinaryService {

    boolean isConfigured();

    /**
     * Uploads a public media image to Cloudinary and returns the absolute HTTPS secure URL.
     *
     * @param file the multipart file to upload
     * @param subfolder subfolder within ngoconnect namespace (e.g. "public", "profiles", "food-drives")
     * @return absolute HTTPS URL (e.g. https://res.cloudinary.com/...)
     * @throws IOException on upload failure
     */
    String uploadImage(MultipartFile file, String subfolder) throws IOException;

    /**
     * Uploads a sensitive verification document to Cloudinary using type 'authenticated',
     * ensuring it is not accessible through public unauthenticated CDN URLs.
     *
     * @param file the multipart file to upload
     * @param subfolder subfolder within ngoconnect/private namespace (e.g. "documents")
     * @return Cloudinary secure URL for the authenticated document
     * @throws IOException on upload failure
     */
    String uploadPrivateDocument(MultipartFile file, String subfolder) throws IOException;

    /**
     * Generates a signed, time-limited delivery URL for an authenticated Cloudinary document asset.
     * Defaults to a 300-second (5-minute) expiration window.
     *
     * @param rawUrlOrPublicId stored document URL or Cloudinary public ID
     * @return signed delivery URL with HMAC signature and expiration timestamp, or null if generation fails
     */
    String generateSignedDocumentUrl(String rawUrlOrPublicId);

    /**
     * Generates a signed, time-limited delivery URL for an authenticated Cloudinary document asset
     * with custom expiration window in seconds.
     *
     * @param rawUrlOrPublicId stored document URL or Cloudinary public ID
     * @param expirationSeconds validity duration in seconds (e.g. 300 for 5 minutes)
     * @return signed delivery URL with HMAC signature and expiration timestamp, or null if generation fails
     */
    String generateSignedDocumentUrl(String rawUrlOrPublicId, int expirationSeconds);
}
