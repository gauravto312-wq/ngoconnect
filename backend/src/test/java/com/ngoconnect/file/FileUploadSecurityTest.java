package com.ngoconnect.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "uploader@test.com", roles = {"CITIZEN"})
public class FileUploadSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("shouldRejectUnauthenticatedUpload - Deny when not logged in")
    @WithAnonymousUser
    void shouldRejectUnauthenticatedUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "unauthenticated file content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldUploadValidJpgImage")
    void shouldUploadValidJpgImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ngo_profile.jpg",
                "image/jpeg",
                "fake image content bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    @DisplayName("shouldUploadValidPngImage")
    void shouldUploadValidPngImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "restaurant_banner.png",
                "image/png",
                "fake png bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists());
    }

    @Test
    @DisplayName("shouldUploadValidWebpImage")
    void shouldUploadValidWebpImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "food_drive.webp",
                "image/webp",
                "fake webp bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists());
    }

    @Test
    @DisplayName("shouldRejectExecutableFileUpload - Block .exe files")
    void shouldRejectExecutableFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "MZ...binary payload".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type. Only image files (JPG, PNG, WEBP) are allowed."));
    }

    @Test
    @DisplayName("shouldRejectInvalidExtensionWithImageMimeType - Block .jsp/.sh extensions")
    void shouldRejectInvalidExtensionWithImageMimeType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "webshell.jsp",
                "image/jpeg",
                "<% runtime.exec(...) %>".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file extension. Allowed extensions: JPG, JPEG, PNG, WEBP"));
    }

    @Test
    @DisplayName("shouldRejectOversizedFile - Exceed 5MB Limit")
    void shouldRejectOversizedFile() throws Exception {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large_photo.jpg",
                "image/jpeg",
                largeContent
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File size exceeds maximum allowed limit of 5MB"));
    }

    @Test
    @DisplayName("shouldRejectEmptyFile")
    void shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is empty or missing"));
    }

    @Test
    @DisplayName("shouldUploadFileLargerThanOneMegabyteWithinFiveMegabytes")
    void shouldUploadFileLargerThanOneMegabyteWithinFiveMegabytes() throws Exception {
        byte[] content = new byte[2 * 1024 * 1024 + 500 * 1024]; // 2.5MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "high_res_camera_photo.jpg",
                "image/jpeg",
                content
        );

        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }
}
