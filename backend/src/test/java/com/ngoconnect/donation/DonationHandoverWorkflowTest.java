package com.ngoconnect.donation;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.Volunteer;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.repository.NgoRepository;
import com.ngoconnect.repository.NotificationRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.repository.VolunteerRepository;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.DonationService;
import com.ngoconnect.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class DonationHandoverWorkflowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DonationService donationService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.ngoconnect.repository.RoleRepository roleRepository;

    private AuthResponse restaurantDonorAuth;
    private AuthResponse citizenDonorAuth;
    private AuthResponse claimingNgoAuth;
    private AuthResponse unrelatedNgoAuth;
    private AuthResponse volunteerAuth;
    private com.ngoconnect.entity.User adminUser;
    private Volunteer volunteer;

    @BeforeEach
    void setUp() {
        // Register Restaurant Donor
        RegisterRequest restReq = new RegisterRequest();
        restReq.setName("Aman Restaurant");
        restReq.setEmail("aman.restaurant@ngoconnect.org");
        restReq.setPhone("9876543101");
        restReq.setPassword("Password123!");
        restReq.setRole("RESTAURANT");
        restReq.setRestaurantName("Aman Kitchen");
        restReq.setCity("Lucknow");
        restReq.setAddress("Hazratganj");
        restaurantDonorAuth = authService.register(restReq);

        // Register Citizen Donor
        RegisterRequest citizenReq = new RegisterRequest();
        citizenReq.setName("Citizen Neha");
        citizenReq.setEmail("neha.citizen@ngoconnect.org");
        citizenReq.setPhone("9876543102");
        citizenReq.setPassword("Password123!");
        citizenReq.setRole("CITIZEN");
        citizenReq.setCity("Lucknow");
        citizenReq.setAddress("Aliganj");
        citizenDonorAuth = authService.register(citizenReq);

        // Register Claiming NGO
        RegisterRequest ngo1Req = new RegisterRequest();
        ngo1Req.setName("Care Shelter NGO");
        ngo1Req.setEmail("care.shelter@ngoconnect.org");
        ngo1Req.setPhone("9876543103");
        ngo1Req.setPassword("Password123!");
        ngo1Req.setRole("NGO");
        ngo1Req.setNgoName("Care Shelter");
        ngo1Req.setCity("Lucknow");
        ngo1Req.setAddress("Indira Nagar");
        claimingNgoAuth = authService.register(ngo1Req);

        userRepository.findByEmail("care.shelter@ngoconnect.org").ifPresent(u -> {
            ngoRepository.findByUserId(u.getId()).ifPresent(ngo -> {
                ngo.setVerificationStatus("APPROVED");
                ngoRepository.save(ngo);
            });
        });

        // Register Unrelated NGO
        RegisterRequest ngo2Req = new RegisterRequest();
        ngo2Req.setName("Helping Souls NGO");
        ngo2Req.setEmail("helping.souls@ngoconnect.org");
        ngo2Req.setPhone("9876543104");
        ngo2Req.setPassword("Password123!");
        ngo2Req.setRole("NGO");
        ngo2Req.setNgoName("Helping Souls");
        ngo2Req.setCity("Lucknow");
        ngo2Req.setAddress("Gomti Nagar");
        unrelatedNgoAuth = authService.register(ngo2Req);

        userRepository.findByEmail("helping.souls@ngoconnect.org").ifPresent(u -> {
            ngoRepository.findByUserId(u.getId()).ifPresent(ngo -> {
                ngo.setVerificationStatus("APPROVED");
                ngoRepository.save(ngo);
            });
        });

        // Register Volunteer
        RegisterRequest volReq = new RegisterRequest();
        volReq.setName("Vikas Volunteer");
        volReq.setEmail("vikas.volunteer@ngoconnect.org");
        volReq.setPhone("9876543105");
        volReq.setPassword("Password123!");
        volReq.setRole("VOLUNTEER");
        volReq.setCity("Lucknow");
        volunteerAuth = authService.register(volReq);

        volunteer = volunteerRepository.findByUserId(volunteerAuth.getId()).orElseThrow();
        volunteer.setAvailabilityStatus("ACTIVE");
        volunteerRepository.save(volunteer);

        // Create Admin User
        com.ngoconnect.entity.Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(com.ngoconnect.entity.Role.builder().name("ADMIN").build()));

        adminUser = userRepository.save(com.ngoconnect.entity.User.builder()
                .name("System Admin")
                .email("system.admin@ngoconnect.org")
                .phone("9876543106")
                .passwordHash("hashed")
                .role(adminRole)
                .isActive(true)
                .build());
    }

    private DonationResponse createSurplusFoodDonation() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Fresh Surplus Biryani");
        req.setDescription("40 warm meals ready for distribution");
        req.setQuantity(40);
        req.setUnit("meals");
        req.setAddress("Hazratganj Main Market");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        return donationService.createDonation(restaurantDonorAuth.getId(), req);
    }

    @Test
    @DisplayName("1. Full End-to-End Handover & Payment Lifecycle")
    void shouldCompleteFullEndToEndHandoverAndPaymentLifecycle() {
        // Step 1: Donor creates donation
        DonationResponse donation = createSurplusFoodDonation();
        assertNotNull(donation.getPickupCode());
        assertEquals("AVAILABLE", donation.getStatus());

        // Step 2: Claiming NGO claims donation
        ClaimResponse claim = donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());
        assertEquals("CLAIMED", claim.getStatus());

        // Step 3: Claiming NGO assigns volunteer for a paid task (Rs 500)
        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Collect Biryani Meals");
        taskReq.setPickupAddress(donation.getAddress());
        taskReq.setDestinationAddress("Indira Nagar Shelter");
        taskReq.setIsPaid(true);
        taskReq.setPaymentAmount(new BigDecimal("500.00"));
        taskReq.setPaymentTerms("Cash or UPI on delivery");

        VolunteerTaskResponse taskResp = taskService.assignTask(claimingNgoAuth.getId(), taskReq);
        assertNotNull(taskResp);
        assertEquals("ASSIGNED", taskResp.getStatus());
        assertEquals("NOT_APPLICABLE", taskResp.getPaymentStatus());
        assertNotNull(taskResp.getPickupCode()); // Volunteer receives the pickup code

        // Verify Donor sees assigned volunteer details
        DonationResponse donorView = donationService.getDonationById(donation.getId(), restaurantDonorAuth.getId());
        assertEquals(volunteer.getId(), donorView.getAssignedVolunteerId());
        assertEquals(volunteer.getUser().getName(), donorView.getAssignedVolunteerName());

        // Step 4: Donor confirms Handover (Stage 1) using the pickup code
        DonationResponse handedOver = donationService.confirmDonorHandover(donation.getId(), donation.getPickupCode(), restaurantDonorAuth.getId());
        assertEquals("PICKUP", handedOver.getStatus());
        assertNotNull(handedOver.getPickedUpAt());

        // Step 5: NGO confirms Receipt (Stage 2) using the SAME pickup code
        DonationResponse completed = donationService.confirmNgoReceipt(donation.getId(), donation.getPickupCode(), claimingNgoAuth.getId());
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getCompletedAt());

        // Verify associated volunteer task transitioned to COMPLETED and paymentStatus transitioned to PENDING
        List<VolunteerTaskResponse> volTasks = taskService.getTasksForVolunteer(volunteerAuth.getId());
        assertEquals(1, volTasks.size());
        VolunteerTaskResponse volTask = volTasks.get(0);
        assertEquals("COMPLETED", volTask.getStatus());
        assertEquals("PENDING", volTask.getPaymentStatus());

        // Step 6: Responsible Payer (Claiming NGO) confirms payment
        VolunteerTaskResponse paidTask = taskService.confirmPayment(volTask.getId(), claimingNgoAuth.getId());
        assertEquals("PAID", paidTask.getPaymentStatus());
        assertNotNull(paidTask.getPaidAt());
        assertEquals(claimingNgoAuth.getId(), paidTask.getPaidByUserId());
    }

    @Test
    @DisplayName("2. Unrelated NGO Blocked from Assigning Volunteer (IDOR Protection)")
    void shouldBlockUnrelatedNgoFromAssigningVolunteer() {
        DonationResponse donation = createSurplusFoodDonation();
        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Unauthorized Assignment");
        taskReq.setPickupAddress(donation.getAddress());

        // Unrelated NGO attempts to assign volunteer
        assertThrows(ForbiddenException.class, () -> taskService.assignTask(unrelatedNgoAuth.getId(), taskReq));
    }

    @Test
    @DisplayName("3. NGO Receipt Blocked Before Donor Confirms Handover")
    void shouldBlockNgoReceiptBeforeDonorHandover() {
        DonationResponse donation = createSurplusFoodDonation();
        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Pickup Task");
        taskReq.setPickupAddress(donation.getAddress());
        taskService.assignTask(claimingNgoAuth.getId(), taskReq);

        // NGO attempts to confirm delivery receipt before donor has confirmed pickup
        assertThrows(BadRequestException.class, () ->
                donationService.confirmNgoReceipt(donation.getId(), donation.getPickupCode(), claimingNgoAuth.getId())
        );
    }

    @Test
    @DisplayName("4. Non-Donor Blocked from First Handover Confirmation")
    void shouldBlockNonDonorFromFirstHandover() {
        DonationResponse donation = createSurplusFoodDonation();
        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Pickup Task");
        taskReq.setPickupAddress(donation.getAddress());
        taskService.assignTask(claimingNgoAuth.getId(), taskReq);

        // Another citizen donor attempts to confirm handover
        assertThrows(ForbiddenException.class, () ->
                donationService.confirmDonorHandover(donation.getId(), donation.getPickupCode(), citizenDonorAuth.getId())
        );

        // Claiming NGO also cannot confirm the first handover (must be donor)
        assertThrows(ForbiddenException.class, () ->
                donationService.confirmDonorHandover(donation.getId(), donation.getPickupCode(), claimingNgoAuth.getId())
        );
    }

    @Test
    @DisplayName("5. Incorrect Pickup Code Rejected for Both Stages")
    void shouldRejectIncorrectPickupCode() {
        DonationResponse donation = createSurplusFoodDonation();
        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Pickup Task");
        taskReq.setPickupAddress(donation.getAddress());
        taskService.assignTask(claimingNgoAuth.getId(), taskReq);

        // Incorrect code at Stage 1
        assertThrows(BadRequestException.class, () ->
                donationService.confirmDonorHandover(donation.getId(), "NGC-WRONG1", restaurantDonorAuth.getId())
        );

        // Successful Stage 1
        donationService.confirmDonorHandover(donation.getId(), donation.getPickupCode(), restaurantDonorAuth.getId());

        // Incorrect code at Stage 2
        assertThrows(BadRequestException.class, () ->
                donationService.confirmNgoReceipt(donation.getId(), "NGC-WRONG2", claimingNgoAuth.getId())
        );
    }

    @Test
    @DisplayName("6. Volunteer Cannot Complete Donation Task Unilaterally")
    void shouldPreventVolunteerFromUnilaterallyCompletingDonationTask() {
        DonationResponse donation = createSurplusFoodDonation();
        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Pickup Task");
        taskReq.setPickupAddress(donation.getAddress());
        VolunteerTaskResponse task = taskService.assignTask(claimingNgoAuth.getId(), taskReq);

        // Volunteer attempts to update task status to COMPLETED before verified NGO receipt
        assertThrows(BadRequestException.class, () ->
                taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteerAuth.getId())
        );
    }

    @Test
    @DisplayName("7. Volunteer Can Dispute Unpaid Pending Payment and Admin Can Resolve")
    void shouldAllowVolunteerToDisputePaymentAndAdminToResolve() {
        DonationResponse donation = createSurplusFoodDonation();
        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Biryani Pickup");
        taskReq.setPickupAddress(donation.getAddress());
        taskReq.setIsPaid(true);
        taskReq.setPaymentAmount(new BigDecimal("350.00"));
        taskReq.setPaymentTerms("Immediate UPI transfer");
        VolunteerTaskResponse task = taskService.assignTask(claimingNgoAuth.getId(), taskReq);

        // Perform two-stage handovers
        donationService.confirmDonorHandover(donation.getId(), donation.getPickupCode(), restaurantDonorAuth.getId());
        donationService.confirmNgoReceipt(donation.getId(), donation.getPickupCode(), claimingNgoAuth.getId());

        // Task is now COMPLETED and paymentStatus is PENDING
        // Volunteer reports dispute because NGO did not transfer payment
        VolunteerTaskResponse disputedTask = taskService.disputePayment(task.getId(), volunteerAuth.getId(), "NGO has not sent payment after 24 hours");
        assertEquals("DISPUTED", disputedTask.getPaymentStatus());
        assertEquals("NGO has not sent payment after 24 hours", disputedTask.getDisputeReason());

        // Admin reviews and resolves dispute by confirming payment
        VolunteerTaskResponse resolvedTask = taskService.resolveDispute(task.getId(), adminUser.getId(), "PAID", "Payment verified via bank UTR number");
        assertEquals("PAID", resolvedTask.getPaymentStatus());
        assertNotNull(resolvedTask.getPaidAt());
        assertEquals(adminUser.getId(), resolvedTask.getPaidByUserId());
    }

    @Test
    @DisplayName("8. Citizen Donor Follows Same Two-Stage Handover Workflow")
    void shouldSupportCitizenDonorHandoverWorkflow() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Homemade Sandwiches");
        req.setQuantity(15);
        req.setUnit("sandwiches");
        req.setAddress("Aliganj Sector B");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(citizenDonorAuth.getId(), req);

        donationService.claimDonation(donation.getId(), claimingNgoAuth.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setDonationId(donation.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Collect Sandwiches");
        taskReq.setPickupAddress(donation.getAddress());
        taskService.assignTask(claimingNgoAuth.getId(), taskReq);

        // Citizen donor confirms Stage 1 handover
        DonationResponse stage1 = donationService.confirmDonorHandover(donation.getId(), donation.getPickupCode(), citizenDonorAuth.getId());
        assertEquals("PICKUP", stage1.getStatus());

        // NGO confirms Stage 2 receipt
        DonationResponse stage2 = donationService.confirmNgoReceipt(donation.getId(), donation.getPickupCode(), claimingNgoAuth.getId());
        assertEquals("COMPLETED", stage2.getStatus());
    }
}
