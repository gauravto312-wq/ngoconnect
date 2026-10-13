package com.ngoconnect.fooddrive;

import com.ngoconnect.dto.*;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.FoodDriveService;
import com.ngoconnect.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FoodDriveAndNotificationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private FoodDriveService foodDriveService;

    @Autowired
    private NotificationService notificationService;

    private AuthResponse hostAuth;
    private AuthResponse participantAuth;

    @BeforeEach
    void setUp() {
        RegisterRequest hostReq = new RegisterRequest();
        hostReq.setName("Community Host");
        hostReq.setEmail("host.fooddrive@ngoconnect.org");
        hostReq.setPhone("9876543210");
        hostReq.setPassword("Password123!");
        hostReq.setRole("CITIZEN");
        hostReq.setCity("Lucknow");
        hostAuth = authService.register(hostReq);

        RegisterRequest partReq = new RegisterRequest();
        partReq.setName("Participant User");
        partReq.setEmail("participant.fooddrive@ngoconnect.org");
        partReq.setPhone("9876543211");
        partReq.setPassword("Password123!");
        partReq.setRole("VOLUNTEER");
        partReq.setCity("Lucknow");
        participantAuth = authService.register(partReq);
    }

    @Test
    @DisplayName("shouldCreateFoodDriveSuccessfully")
    void shouldCreateFoodDriveSuccessfully() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("City Wide Surplus Relief Drive");
        req.setDescription("Distributing meals across shelter homes");
        req.setPurpose("Hunger Relief");
        req.setAddress("Charbagh Railway Station");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        req.setEventDate(LocalDate.now().plusDays(7));
        req.setStartTime("10:00 AM");
        req.setEndTime("04:00 PM");

        FoodDriveResponse resp = foodDriveService.createFoodDrive(hostAuth.getId(), req);

        assertNotNull(resp);
        assertNotNull(resp.getId());
        assertEquals("UPCOMING", resp.getStatus());
        assertEquals("City Wide Surplus Relief Drive", resp.getTitle());
    }

    @Test
    @DisplayName("shouldAllowUserToJoinAndContributeToFoodDrive")
    void shouldAllowUserToJoinAndContributeToFoodDrive() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Winter Surplus Food Drive");
        req.setAddress("Aliganj Park");
        req.setCity("Lucknow");
        req.setEventDate(LocalDate.now().plusDays(5));
        FoodDriveResponse drive = foodDriveService.createFoodDrive(hostAuth.getId(), req);

        FoodDriveParticipantRequest partReq = new FoodDriveParticipantRequest();
        partReq.setContributionDescription("Cooked Meals");
        partReq.setContributionQuantity(100);
        partReq.setContributionUnit("meals");

        FoodDriveParticipantResponse partResp = foodDriveService.joinOrContribute(drive.getId(), participantAuth.getId(), partReq);

        assertNotNull(partResp);
        assertEquals("JOINED", partResp.getStatus());
        assertEquals(100, partResp.getContributionQuantity());

        // Verify host receives notification
        List<NotificationResponse> hostNotifications = notificationService.getUserNotifications(hostAuth.getId());
        assertFalse(hostNotifications.isEmpty());
        assertTrue(hostNotifications.stream().anyMatch(n -> n.getTitle().contains("New Participant")));
    }

    @Test
    @DisplayName("shouldFetchPublicFoodDrivesFilteredByCity")
    void shouldFetchPublicFoodDrivesFilteredByCity() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Lucknow Relief Drive");
        req.setCity("Lucknow");
        foodDriveService.createFoodDrive(hostAuth.getId(), req);

        List<FoodDriveResponse> lucknowDrives = foodDriveService.getFoodDrives("Lucknow");
        assertFalse(lucknowDrives.isEmpty());
        assertEquals("Lucknow", lucknowDrives.get(0).getCity());
    }
}
