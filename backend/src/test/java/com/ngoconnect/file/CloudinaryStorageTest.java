package com.ngoconnect.file;

import com.ngoconnect.service.CloudinaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "uploader@test.com", roles = {"CITIZEN"})
public class CloudinaryStorageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudinaryService cloudinaryService;

    @Autowired
    private com.ngoconnect.service.FileStorageService fileStorageService;

    @Autowired
    private com.ngoconnect.config.UploadRateLimiter rateLimiter;

    @Autowired
    private com.ngoconnect.service.NgoService ngoService;

    @Test
    @DisplayName("shouldUploadSuccessfullyToCloudinary - Return secure HTTPS URL")
    void shouldUploadSuccessfullyToCloudinary() throws Exception {
        String expectedSecureUrl = "https://res.cloudinary.com/test-cloud/image/upload/v12345/ngoconnect/public/profile.jpg";
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadImage(any(), eq("public"))).thenReturn(expectedSecureUrl);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                "sample-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(expectedSecureUrl));

        verify(cloudinaryService, times(1)).uploadImage(any(), eq("public"));
    }

    @Test
    @DisplayName("shouldRejectUnsupportedMimeTypeBeforeReachingCloudinary")
    void shouldRejectUnsupportedMimeTypeBeforeReachingCloudinary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.sh",
                "text/x-shellscript",
                "echo hello".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type. Only image files (JPG, PNG, WEBP) are allowed."));

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("shouldHandleCloudinaryProviderErrorSafely - No credentials or stack leaked")
    void shouldHandleCloudinaryProviderErrorSafely() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(true);
        when(cloudinaryService.uploadImage(any(), eq("public")))
                .thenThrow(new IOException("Connection reset by peer at api.cloudinary.com"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "avatar-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to upload file safely"));
    }

    @Test
    @DisplayName("shouldFallbackToLocalStorageWhenCloudinaryNotConfigured")
    void shouldFallbackToLocalStorageWhenCloudinaryNotConfigured() throws Exception {
        when(cloudinaryService.isConfigured()).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "local_test.jpg",
                "image/jpeg",
                "test-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    @DisplayName("shouldReportUnconfiguredWhenCloudinaryBeanNull")
    void shouldReportUnconfiguredWhenCloudinaryBeanNull() {
        CloudinaryService unconfiguredService = new com.ngoconnect.service.impl.CloudinaryServiceImpl(null);
        assertFalse(unconfiguredService.isConfigured());

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        assertThrows(IllegalStateException.class, () -> unconfiguredService.uploadImage(file, "public"));
    }

    @Test
    @DisplayName("shouldGenerateTimeLimitedSignedDownloadUrlWithExpiration")
    void shouldGenerateTimeLimitedSignedDownloadUrlWithExpiration() {
        com.cloudinary.Cloudinary mockCloudinary = new com.cloudinary.Cloudinary(
                com.cloudinary.utils.ObjectUtils.asMap(
                        "cloud_name", "test-cloud",
                        "api_key", "123456789012345",
                        "api_secret", "abcdefsecret9876543210"
                )
        );
        com.ngoconnect.service.impl.CloudinaryServiceImpl service =
                new com.ngoconnect.service.impl.CloudinaryServiceImpl(mockCloudinary);

        String rawUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf";
        String signedUrl = service.generateSignedDocumentUrl(rawUrl, 300);

        org.junit.jupiter.api.Assertions.assertNotNull(signedUrl);
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.startsWith("https://api.cloudinary.com/v1_1/test-cloud/image/download"));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("expires_at="));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("signature="));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("timestamp="));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("api_key=123456789012345"));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("public_id=ngoconnect%2Fprivate%2Fdocuments%2Fcert"));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("format=pdf"));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("type=authenticated"));
    }

    @Test
    @DisplayName("shouldNotTrustExistingExpiresAtParameterAndRecomputeFreshSignature")
    void shouldNotTrustExistingExpiresAtParameterAndRecomputeFreshSignature() {
        com.cloudinary.Cloudinary mockCloudinary = new com.cloudinary.Cloudinary(
                com.cloudinary.utils.ObjectUtils.asMap(
                        "cloud_name", "test-cloud",
                        "api_key", "123456789012345",
                        "api_secret", "abcdefsecret9876543210"
                )
        );
        com.ngoconnect.service.impl.CloudinaryServiceImpl service =
                new com.ngoconnect.service.impl.CloudinaryServiceImpl(mockCloudinary);

        // Untrusted URL with spoofed expires_at and fake signature
        String tamperedUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf?expires_at=9999999999&signature=spoofedSig";
        String signedUrl = service.generateSignedDocumentUrl(tamperedUrl, 300);

        org.junit.jupiter.api.Assertions.assertNotNull(signedUrl);
        org.junit.jupiter.api.Assertions.assertFalse(signedUrl.contains("expires_at=9999999999"), "Pre-existing expires_at must not be trusted");
        org.junit.jupiter.api.Assertions.assertFalse(signedUrl.contains("signature=spoofedSig"), "Pre-existing signature must not be trusted");
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("expires_at="));
        org.junit.jupiter.api.Assertions.assertTrue(signedUrl.contains("signature="));
    }

    @Test
    @DisplayName("shouldRejectPublicUploadPathForPrivateDocument")
    void shouldRejectPublicUploadPathForPrivateDocument() {
        com.cloudinary.Cloudinary mockCloudinary = new com.cloudinary.Cloudinary(
                com.cloudinary.utils.ObjectUtils.asMap(
                        "cloud_name", "test-cloud",
                        "api_key", "123456789012345",
                        "api_secret", "abcdefsecret9876543210"
                )
        );
        com.ngoconnect.service.impl.CloudinaryServiceImpl service =
                new com.ngoconnect.service.impl.CloudinaryServiceImpl(mockCloudinary);

        // URL uses public /upload/ instead of /authenticated/
        String publicUrl = "https://res.cloudinary.com/test-cloud/image/upload/v12345/ngoconnect/private/documents/cert.pdf";
        String signedUrl = service.generateSignedDocumentUrl(publicUrl, 300);

        org.junit.jupiter.api.Assertions.assertNull(signedUrl, "Public /upload/ paths must be rejected for private documents");
    }

    @Test
    @DisplayName("shouldRejectPathTraversalInDocumentIdentifier")
    void shouldRejectPathTraversalInDocumentIdentifier() {
        com.cloudinary.Cloudinary mockCloudinary = new com.cloudinary.Cloudinary(
                com.cloudinary.utils.ObjectUtils.asMap(
                        "cloud_name", "test-cloud",
                        "api_key", "123456789012345",
                        "api_secret", "abcdefsecret9876543210"
                )
        );
        com.ngoconnect.service.impl.CloudinaryServiceImpl service =
                new com.ngoconnect.service.impl.CloudinaryServiceImpl(mockCloudinary);

        org.junit.jupiter.api.Assertions.assertNull(service.generateSignedDocumentUrl("../../etc/passwd", 300));
        org.junit.jupiter.api.Assertions.assertNull(service.generateSignedDocumentUrl("https://res.cloudinary.com/test-cloud/image/authenticated/../../etc/passwd", 300));
        org.junit.jupiter.api.Assertions.assertNull(service.generateSignedDocumentUrl("", 300));
        org.junit.jupiter.api.Assertions.assertNull(service.generateSignedDocumentUrl(null, 300));
    }

    @Test
    @DisplayName("shouldReturnNullWhenUrlGenerationFailsWithoutReturningRawUrlOrNonExpiringFallback")
    void shouldReturnNullWhenUrlGenerationFailsWithoutReturningRawUrlOrNonExpiringFallback() {
        // Missing api_secret so privateDownload throws IllegalArgumentException
        com.cloudinary.Cloudinary brokenCloudinary = new com.cloudinary.Cloudinary(
                com.cloudinary.utils.ObjectUtils.asMap(
                        "cloud_name", "test-cloud",
                        "api_key", "123456789012345"
                )
        );
        com.ngoconnect.service.impl.CloudinaryServiceImpl service =
                new com.ngoconnect.service.impl.CloudinaryServiceImpl(brokenCloudinary);

        String rawUrl = "https://res.cloudinary.com/test-cloud/image/authenticated/v12345/ngoconnect/private/documents/cert.pdf";
        String result = service.generateSignedDocumentUrl(rawUrl, 300);

        org.junit.jupiter.api.Assertions.assertNull(result, "Must return null on failure, never rawUrl or unverified fallback");
    }

    @Test
    @DisplayName("shouldRejectUploadWith503WhenCloudinaryUnconfiguredAndLocalFallbackDisabled")
    void shouldRejectUploadWith503WhenCloudinaryUnconfiguredAndLocalFallbackDisabled() {
        com.ngoconnect.controller.FileUploadController controller =
                new com.ngoconnect.controller.FileUploadController(fileStorageService, cloudinaryService, rateLimiter);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "allowLocalFallback", false);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "avatar-bytes".getBytes()
        );

        org.springframework.http.ResponseEntity<?> response = controller.uploadFile(file, "test-user");
        org.junit.jupiter.api.Assertions.assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> body = (java.util.Map<String, String>) response.getBody();
        org.junit.jupiter.api.Assertions.assertNotNull(body);
        org.junit.jupiter.api.Assertions.assertTrue(body.get("message").contains("Persistent media storage is not configured"));
    }

    @Test
    @DisplayName("shouldRejectNgoDocUploadWith503WhenCloudinaryUnconfiguredAndLocalFallbackDisabled")
    void shouldRejectNgoDocUploadWith503WhenCloudinaryUnconfiguredAndLocalFallbackDisabled() {
        com.ngoconnect.controller.NgoController ngoController =
                new com.ngoconnect.controller.NgoController(ngoService, fileStorageService, cloudinaryService, rateLimiter);
        org.springframework.test.util.ReflectionTestUtils.setField(ngoController, "allowLocalFallback", false);
        when(cloudinaryService.isConfigured()).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cert.pdf",
                "application/pdf",
                "pdf-bytes".getBytes()
        );

        com.ngoconnect.security.UserPrincipal principal = new com.ngoconnect.security.UserPrincipal(
                999L, "NGO Rep", "ngo@test.com", "pass", "NGO", java.util.Collections.emptyList(), true
        );

        org.springframework.http.ResponseEntity<?> response = ngoController.uploadVerificationDocument(file, principal);
        org.junit.jupiter.api.Assertions.assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> body = (java.util.Map<String, String>) response.getBody();
        org.junit.jupiter.api.Assertions.assertNotNull(body);
        org.junit.jupiter.api.Assertions.assertTrue(body.get("message").contains("Persistent media storage is not configured"));
    }
}
