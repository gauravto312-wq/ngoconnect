package com.ngoconnect.service;

import com.ngoconnect.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FoodDriveAndAdminServiceTests {

    @Autowired
    private FoodDriveService foodDriveService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private NgoService ngoService;

    @Autowired
    private AuthService authService;

    private AuthResponse registerUser(String name, String email, String role, String city) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPhone("9876543210");
        req.setPassword("Password123");
        req.setRole(role);
        req.setAddress("Test Address");
        req.setCity(city);
        req.setState("Uttar Pradesh");
        req.setNgoName(role.equals("NGO") ? name : null);
        return authService.register(req);
    }

    @Test
    @DisplayName("Phase 24 & 31: Community Food Drive Creation & Participation")
    void testFoodDriveWorkflow() {
        AuthResponse citizen = registerUser("Citizen Host", "host.citizen@test.com", "CITIZEN", "Lucknow");
        AuthResponse ngo = registerUser("NGO Contributor", "ngo.contrib@test.com", "NGO", "Lucknow");

        FoodDriveRequest driveReq = new FoodDriveRequest();
        driveReq.setTitle("BBD Slum Food Drive");
        driveReq.setPurpose("Weekend Meal Distribution");
        driveReq.setDescription("Distributing thalis to slum dwellers.");
        driveReq.setAddress("Faizabad Road");
        driveReq.setCity("Lucknow");
        driveReq.setState("Uttar Pradesh");
        driveReq.setEventDate(LocalDate.now().plusDays(5));
        driveReq.setStartTime("10:00 AM");
        driveReq.setEndTime("02:00 PM");

        FoodDriveResponse drive = foodDriveService.createFoodDrive(citizen.getId(), driveReq);
        assertNotNull(drive);
        assertEquals("UPCOMING", drive.getStatus());

        // NGO joins and contributes 100 meals
        FoodDriveParticipantRequest partReq = new FoodDriveParticipantRequest();
        partReq.setContributionDescription("Purbanchal Meal Packets");
        partReq.setContributionQuantity(100);
        partReq.setContributionUnit("meals");

        FoodDriveParticipantResponse participant = foodDriveService.joinOrContribute(drive.getId(), ngo.getId(), partReq);
        assertNotNull(participant);
        assertEquals(100, participant.getContributionQuantity());

        FoodDriveResponse updatedDrive = foodDriveService.getFoodDriveById(drive.getId());
        assertEquals(1, updatedDrive.getParticipantCount());
        assertEquals(100, updatedDrive.getTotalMealsContributed());
    }

    @Autowired
    private com.ngoconnect.repository.UserRepository userRepository;
    @Autowired
    private com.ngoconnect.repository.RoleRepository roleRepository;

    @Test
    @DisplayName("Phase 17 & 29: Admin NGO Verification Review & Dashboard Metrics")
    void testAdminVerificationAndStats() {
        com.ngoconnect.entity.Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(com.ngoconnect.entity.Role.builder().name("ADMIN").build()));

        com.ngoconnect.entity.User adminUser = userRepository.save(com.ngoconnect.entity.User.builder()
                .name("Admin User")
                .email("admin.teststat@ngoconnect.org")
                .phone("9999999999")
                .passwordHash("hashed")
                .role(adminRole)
                .isActive(true)
                .build());

        AuthResponse ngoUser = registerUser("Unverified NGO", "unverif.ngo@test.com", "NGO", "Lucknow");

        // NGO submits document
        NgoVerificationRequest docReq = new NgoVerificationRequest();
        docReq.setDocumentType("80G Certificate");
        docReq.setDocumentNumber("80G-847291");
        docReq.setDocumentUrl("https://docs.test.org/80g.pdf");

        NgoVerificationResponse docResp = ngoService.submitVerificationDocument(ngoUser.getId(), docReq);
        assertNotNull(docResp);
        assertEquals("PENDING", docResp.getVerificationStatus());

        // Admin checks pending verifications
        List<NgoVerificationResponse> pendingList = adminService.getPendingNgoVerifications();
        assertFalse(pendingList.isEmpty());

        // Admin approves NGO verification
        NgoVerificationResponse reviewed = adminService.reviewNgoVerification(docResp.getId(), adminUser.getId(), "APPROVED", "Document verified");
        assertEquals("APPROVED", reviewed.getVerificationStatus());

        // NGO profile is now APPROVED
        NgoProfileResponse ngoProf = ngoService.getNgoProfile(ngoUser.getId());
        assertEquals("APPROVED", ngoProf.getVerificationStatus());

        // Admin dashboard stats
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalUsers() > 0);
    }
}
