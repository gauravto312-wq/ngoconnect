package com.ngoconnect.security;

import com.ngoconnect.NGOConnectApplication;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NGOConnectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityRemediationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private String validToken;
    private Long registeredUserId;

    @BeforeEach
    void setUp() {
        Role citizenRole = roleRepository.findByName("CITIZEN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CITIZEN").build()));

        RegisterRequest req = new RegisterRequest();
        req.setName("Security Audit Citizen");
        req.setEmail("sec.audit.citizen@ngoconnect.org");
        req.setPhone("9888877777");
        req.setPassword("Password123!");
        req.setRole("CITIZEN");

        AuthResponse resp = authService.register(req);
        validToken = resp.getToken();

        User user = userRepository.findByEmail("sec.audit.citizen@ngoconnect.org").orElseThrow();
        registeredUserId = user.getId();
    }

    @Test
    @DisplayName("shouldAllowAccessForActiveUserWithValidJwt")
    void shouldAllowAccessForActiveUserWithValidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/donations/my-donations")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("shouldDenyAccessWhenUserIsDeactivatedEvenWithValidJwt")
    void shouldDenyAccessWhenUserIsDeactivatedEvenWithValidJwt() throws Exception {
        // Deactivate user in repository
        User user = userRepository.findById(registeredUserId).orElseThrow();
        user.setIsActive(false);
        userRepository.saveAndFlush(user);

        // Attempt request with previously valid JWT token
        mockMvc.perform(get("/api/v1/donations/my-donations")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRequireAuthenticationForUploadEndpoint")
    void shouldRequireAuthenticationForUploadEndpoint() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "sample data".getBytes()
        );

        // Unauthenticated request must be rejected
        mockMvc.perform(multipart("/api/v1/upload").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldEnforceUploadRateLimitingAfterLimitExceeded")
    void shouldEnforceUploadRateLimitingAfterLimitExceeded() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "sample bytes".getBytes()
        );

        // Perform 20 successful uploads within rate limit
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(multipart("/api/v1/upload")
                    .file(file)
                    .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk());
        }

        // 21st upload within 1 minute must return 429 Too Many Requests
        mockMvc.perform(multipart("/api/v1/upload")
                .file(file)
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Upload quota exceeded. Maximum 20 uploads per minute allowed. Please try again later."));
    }
}
