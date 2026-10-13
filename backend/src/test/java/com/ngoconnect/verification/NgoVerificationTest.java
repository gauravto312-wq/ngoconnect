package com.ngoconnect.verification;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.Ngo;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.NgoRepository;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.security.UserPrincipal;
import com.ngoconnect.service.AdminService;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.NgoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class NgoVerificationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private NgoService ngoService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private AuthResponse ngoAuth;

    @BeforeEach
    void setUp() {
        // Create Admin user
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        adminUser = userRepository.save(User.builder()
                .name("Super Admin")
                .email("admin.verification@ngoconnect.org")
                .phone("9999999999")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(adminRole)
                .isActive(true)
                .build());

        // Register NGO
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Care Foundation Representative");
        regReq.setEmail("care.foundation@ngoconnect.org");
        regReq.setPhone("9876543210");
        regReq.setPassword("Password123!");
        regReq.setRole("NGO");
        regReq.setNgoName("Care Foundation NGO");
        regReq.setCause("Child Welfare");
        regReq.setCity("Lucknow");
        regReq.setAddress("Aliganj Sector B");

        ngoAuth = authService.register(regReq);
    }

    @Test
    @DisplayName("shouldCreateNgoWithPendingVerificationStatus")
    void shouldCreateNgoWithPendingVerificationStatus() {
        NgoProfileResponse profile = ngoService.getNgoProfile(ngoAuth.getId());

        assertNotNull(profile);
        assertEquals("Care Foundation NGO", profile.getNgoName());
        assertEquals("PENDING", profile.getVerificationStatus());
    }

    @Test
    @DisplayName("shouldSubmitVerificationDocumentAndReceivePendingStatus")
    void shouldSubmitVerificationDocumentAndReceivePendingStatus() {
        NgoVerificationRequest verifReq = new NgoVerificationRequest();
        verifReq.setDocumentType("TRUST_ACT_REGISTRATION");
        verifReq.setDocumentUrl("/uploads/cert123.jpg");
        verifReq.setDocumentNumber("REG-12345-UP");

        NgoVerificationResponse verifResp = ngoService.submitVerificationDocument(ngoAuth.getId(), verifReq);

        assertNotNull(verifResp);
        assertEquals("PENDING", verifResp.getVerificationStatus());
        assertEquals("REG-12345-UP", verifResp.getDocumentNumber());
    }

    @Test
    @DisplayName("shouldAllowAdminToApproveNgo")
    void shouldAllowAdminToApproveNgo() {
        Ngo ngo = ngoRepository.findByUserId(ngoAuth.getId()).orElseThrow();

        NgoProfileResponse updatedProfile = adminService.reviewNgoStatus(ngo.getId(), adminUser.getId(), "APPROVED");

        assertNotNull(updatedProfile);
        assertEquals("APPROVED", updatedProfile.getVerificationStatus());

        // Verify DB update
        Ngo updatedDbNgo = ngoRepository.findById(ngo.getId()).orElseThrow();
        assertEquals("APPROVED", updatedDbNgo.getVerificationStatus());
    }

    @Test
    @DisplayName("shouldAllowAdminToRejectNgo")
    void shouldAllowAdminToRejectNgo() {
        Ngo ngo = ngoRepository.findByUserId(ngoAuth.getId()).orElseThrow();

        NgoProfileResponse updatedProfile = adminService.reviewNgoStatus(ngo.getId(), adminUser.getId(), "REJECTED");

        assertNotNull(updatedProfile);
        assertEquals("REJECTED", updatedProfile.getVerificationStatus());

        // Verify DB update
        Ngo updatedDbNgo = ngoRepository.findById(ngo.getId()).orElseThrow();
        assertEquals("REJECTED", updatedDbNgo.getVerificationStatus());
    }

    @Test
    @DisplayName("shouldAllowAdminToReviewVerificationDocument")
    void shouldAllowAdminToReviewVerificationDocument() {
        NgoVerificationRequest verifReq = new NgoVerificationRequest();
        verifReq.setDocumentType("80G_CERTIFICATE");
        verifReq.setDocumentUrl("/uploads/80g.pdf");
        verifReq.setDocumentNumber("80G-998877");

        NgoVerificationResponse verifResp = ngoService.submitVerificationDocument(ngoAuth.getId(), verifReq);

        NgoVerificationResponse reviewed = adminService.reviewNgoVerification(
                verifResp.getId(),
                adminUser.getId(),
                "APPROVED",
                "Documents verified successfully."
        );

        assertNotNull(reviewed);
        assertEquals("APPROVED", reviewed.getVerificationStatus());
        assertEquals("Super Admin", reviewed.getAdminName());
        assertEquals("Documents verified successfully.", reviewed.getRemarks());
    }
}
