package com.ngoconnect.task;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.Volunteer;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.repository.VolunteerRepository;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.TaskService;
import com.ngoconnect.service.VolunteerService;
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
public class PaidVolunteerTaskTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    private AuthResponse ngoAuth;
    private AuthResponse restaurantAuth;
    private AuthResponse volunteerAuth;
    private AuthResponse otherVolunteerAuth;
    private Volunteer volunteer;

    @BeforeEach
    void setUp() {
        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("Care Foundation NGO");
        ngoReq.setEmail("care.foundation@ngoconnect.org");
        ngoReq.setPhone("9876543201");
        ngoReq.setPassword("Password123!");
        ngoReq.setRole("NGO");
        ngoReq.setNgoName("Care Foundation");
        ngoAuth = authService.register(ngoReq);

        RegisterRequest restReq = new RegisterRequest();
        restReq.setName("Bakehouse Restaurant");
        restReq.setEmail("bakehouse@ngoconnect.org");
        restReq.setPhone("9876543202");
        restReq.setPassword("Password123!");
        restReq.setRole("RESTAURANT");
        restaurantAuth = authService.register(restReq);

        RegisterRequest volReq = new RegisterRequest();
        volReq.setName("Ravi Volunteer");
        volReq.setEmail("ravi.volunteer@ngoconnect.org");
        volReq.setPhone("9876543203");
        volReq.setPassword("Password123!");
        volReq.setRole("VOLUNTEER");
        volReq.setCity("Lucknow");
        volunteerAuth = authService.register(volReq);

        RegisterRequest otherVolReq = new RegisterRequest();
        otherVolReq.setName("Sara Volunteer");
        otherVolReq.setEmail("sara.volunteer@ngoconnect.org");
        otherVolReq.setPhone("9876543204");
        otherVolReq.setPassword("Password123!");
        otherVolReq.setRole("VOLUNTEER");
        otherVolReq.setCity("Lucknow");
        otherVolunteerAuth = authService.register(otherVolReq);

        volunteer = volunteerRepository.findByUserId(volunteerAuth.getId()).orElseThrow();
    }

    @Test
    @DisplayName("1. shouldCreateUnpaidTaskWithStatusNotApplicable")
    void shouldCreateUnpaidTaskWithStatusNotApplicable() {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Unpaid Surplus Pickup");
        req.setPickupAddress("Bakehouse Hazratganj");
        req.setDestinationAddress("Care Shelter Aliganj");
        req.setScheduledTime(LocalDateTime.now().plusHours(2));
        req.setIsPaid(false);

        VolunteerTaskResponse resp = taskService.assignTask(ngoAuth.getId(), req);

        assertNotNull(resp);
        assertFalse(resp.getIsPaid());
        assertEquals("NOT_APPLICABLE", resp.getPaymentStatus());
        assertNull(resp.getPaymentAmount());
        assertEquals("ASSIGNED", resp.getStatus());
    }

    @Test
    @DisplayName("2. shouldCreatePaidTaskWithValidAmount")
    void shouldCreatePaidTaskWithValidAmount() {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Paid Express Surplus Transport");
        req.setPickupAddress("Bakehouse Gomti Nagar");
        req.setDestinationAddress("Shelter Vikas Nagar");
        req.setScheduledTime(LocalDateTime.now().plusHours(2));
        req.setIsPaid(true);
        req.setPaymentAmount(new BigDecimal("500.00"));
        req.setCurrency("INR");
        req.setPaymentTerms("Pay upon handover confirmation");

        VolunteerTaskResponse resp = taskService.assignTask(restaurantAuth.getId(), req);

        assertNotNull(resp);
        assertTrue(resp.getIsPaid());
        assertEquals(new BigDecimal("500.00"), resp.getPaymentAmount());
        assertEquals("INR", resp.getCurrency());
        assertEquals("Pay upon handover confirmation", resp.getPaymentTerms());
        assertEquals("NOT_APPLICABLE", resp.getPaymentStatus()); // NOT_APPLICABLE until verified completed
        assertEquals("ASSIGNED", resp.getStatus());
    }

    @Test
    @DisplayName("3. shouldRejectPaidTaskWithNegativeOrZeroOrMissingAmount")
    void shouldRejectPaidTaskWithNegativeOrZeroOrMissingAmount() {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Invalid Paid Task");
        req.setPickupAddress("Hazratganj");
        req.setDestinationAddress("Aliganj");
        req.setIsPaid(true);

        // Missing amount
        req.setPaymentAmount(null);
        assertThrows(BadRequestException.class, () -> taskService.assignTask(ngoAuth.getId(), req));

        // Zero amount
        req.setPaymentAmount(BigDecimal.ZERO);
        assertThrows(BadRequestException.class, () -> taskService.assignTask(ngoAuth.getId(), req));

        // Negative amount
        req.setPaymentAmount(new BigDecimal("-150.00"));
        assertThrows(BadRequestException.class, () -> taskService.assignTask(ngoAuth.getId(), req));
    }

    @Test
    @DisplayName("4. shouldTransitionPaymentStatusToPendingOnlyAfterCompletion")
    void shouldTransitionPaymentStatusToPendingOnlyAfterCompletion() {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Delivery with Payment Transition");
        req.setPickupAddress("Pickup Spot");
        req.setDestinationAddress("Drop Spot");
        req.setIsPaid(true);
        req.setPaymentAmount(new BigDecimal("400.00"));

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), req);
        assertEquals("NOT_APPLICABLE", task.getPaymentStatus());

        // Accept
        VolunteerTaskResponse accepted = taskService.updateTaskStatus(task.getId(), "ACCEPTED", volunteerAuth.getId());
        assertEquals("NOT_APPLICABLE", accepted.getPaymentStatus());

        // In Progress
        VolunteerTaskResponse inProgress = taskService.updateTaskStatus(task.getId(), "IN_PROGRESS", volunteerAuth.getId());
        assertEquals("NOT_APPLICABLE", inProgress.getPaymentStatus());

        // Completed -> Transitions to PENDING
        VolunteerTaskResponse completed = taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteerAuth.getId());
        assertEquals("PENDING", completed.getPaymentStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    @DisplayName("5. shouldAllowAssignerToManuallyConfirmPayment")
    void shouldAllowAssignerToManuallyConfirmPayment() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("600.00"));
        assertEquals("PENDING", task.getPaymentStatus());

        VolunteerTaskResponse paid = taskService.confirmPayment(task.getId(), ngoAuth.getId());
        assertEquals("PAID", paid.getPaymentStatus());
        assertNotNull(paid.getPaidAt());
        assertEquals(ngoAuth.getId(), paid.getPaidByUserId());
    }

    @Test
    @DisplayName("6. shouldRejectVolunteerMarkingOwnTaskAsPaid")
    void shouldRejectVolunteerMarkingOwnTaskAsPaid() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("600.00"));

        // Volunteer attempts to confirm their own payment -> MUST THROW 403 FORBIDDEN
        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                taskService.confirmPayment(task.getId(), volunteerAuth.getId())
        );
        assertTrue(ex.getMessage().contains("Volunteers cannot mark their own tasks as paid"));
    }

    @Test
    @DisplayName("7. shouldRejectUnrelatedUserConfirmingPayment")
    void shouldRejectUnrelatedUserConfirmingPayment() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("600.00"));

        // Another volunteer or unrelated user attempts to confirm payment -> MUST THROW 403 FORBIDDEN
        assertThrows(ForbiddenException.class, () ->
                taskService.confirmPayment(task.getId(), otherVolunteerAuth.getId())
        );
    }

    @Test
    @DisplayName("8. shouldRejectDuplicatePaymentConfirmation")
    void shouldRejectDuplicatePaymentConfirmation() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("600.00"));
        taskService.confirmPayment(task.getId(), ngoAuth.getId());

        // Second confirmation attempt must be rejected
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                taskService.confirmPayment(task.getId(), ngoAuth.getId())
        );
        assertTrue(ex.getMessage().contains("already been confirmed as PAID"));
    }

    @Test
    @DisplayName("9. shouldRejectPaymentConfirmationForUncompletedTask")
    void shouldRejectPaymentConfirmationForUncompletedTask() {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Active Uncompleted Task");
        req.setPickupAddress("Pickup Spot");
        req.setDestinationAddress("Drop Spot");
        req.setIsPaid(true);
        req.setPaymentAmount(new BigDecimal("300.00"));

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), req);

        // Task is ASSIGNED, not COMPLETED -> cannot confirm payment
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                taskService.confirmPayment(task.getId(), ngoAuth.getId())
        );
        assertTrue(ex.getMessage().contains("Payment can only be confirmed for COMPLETED tasks"));
    }

    @Test
    @DisplayName("10. shouldRejectPaymentConfirmationForUnpaidTask")
    void shouldRejectPaymentConfirmationForUnpaidTask() {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Unpaid Completed Task");
        req.setPickupAddress("Pickup Spot");
        req.setDestinationAddress("Drop Spot");
        req.setIsPaid(false);

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), req);
        taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteerAuth.getId());

        // Unpaid task cannot be confirmed as paid
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                taskService.confirmPayment(task.getId(), ngoAuth.getId())
        );
        assertTrue(ex.getMessage().contains("Cannot confirm payment for an unpaid task"));
    }

    @Test
    @DisplayName("11. shouldAllowAssignerToDisputePayment")
    void shouldAllowAssignerToDisputePayment() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("450.00"));

        VolunteerTaskResponse disputed = taskService.disputePayment(task.getId(), ngoAuth.getId(), "Food containers were damaged");
        assertEquals("DISPUTED", disputed.getPaymentStatus());
        assertNotNull(disputed.getDisputedAt());
        assertEquals("Food containers were damaged", disputed.getDisputeReason());
    }

    @Test
    @DisplayName("12. shouldRejectDisputeWithoutReasonOrOnAlreadyPaidTask")
    void shouldRejectDisputeWithoutReasonOrOnAlreadyPaidTask() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("450.00"));

        // Blank reason
        assertThrows(BadRequestException.class, () ->
                taskService.disputePayment(task.getId(), ngoAuth.getId(), "  ")
        );

        // Paid task cannot be disputed
        taskService.confirmPayment(task.getId(), ngoAuth.getId());
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                taskService.disputePayment(task.getId(), ngoAuth.getId(), "Late delivery")
        );
        assertTrue(ex.getMessage().contains("Cannot dispute an already PAID task"));
    }

    @Test
    @DisplayName("13. shouldAllowResolvingDisputeByConfirmingPayment")
    void shouldAllowResolvingDisputeByConfirmingPayment() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("700.00"));
        taskService.disputePayment(task.getId(), ngoAuth.getId(), "Verification pending");

        // Assigner resolves dispute and confirms payment
        VolunteerTaskResponse resolved = taskService.confirmPayment(task.getId(), ngoAuth.getId());
        assertEquals("PAID", resolved.getPaymentStatus());
        assertNotNull(resolved.getPaidAt());
    }

    @Test
    @DisplayName("14. shouldCalculateVolunteerEarningsAccuratelyFromPersistedRecords")
    void shouldCalculateVolunteerEarningsAccuratelyFromPersistedRecords() {
        // 1. Task PAID for 500
        VolunteerTaskResponse t1 = createAndCompletePaidTask(new BigDecimal("500.00"));
        taskService.confirmPayment(t1.getId(), ngoAuth.getId());

        // 2. Task PAID for 350
        VolunteerTaskResponse t2 = createAndCompletePaidTask(new BigDecimal("350.00"));
        taskService.confirmPayment(t2.getId(), ngoAuth.getId());

        // 3. Task PENDING for 200 (completed but not confirmed paid)
        VolunteerTaskResponse t3 = createAndCompletePaidTask(new BigDecimal("200.00"));

        // 4. Task DISPUTED for 150
        VolunteerTaskResponse t4 = createAndCompletePaidTask(new BigDecimal("150.00"));
        taskService.disputePayment(t4.getId(), ngoAuth.getId(), "Quantity mismatch");

        // 5. Unpaid task completed for 0 (must not affect earnings)
        VolunteerTaskRequest unpaidReq = new VolunteerTaskRequest();
        unpaidReq.setVolunteerId(volunteer.getId());
        unpaidReq.setTaskType("PICKUP_DONATION");
        unpaidReq.setTitle("Unpaid Voluntary Effort");
        unpaidReq.setPickupAddress("A");
        unpaidReq.setDestinationAddress("B");
        unpaidReq.setIsPaid(false);
        VolunteerTaskResponse unpaid = taskService.assignTask(ngoAuth.getId(), unpaidReq);
        taskService.updateTaskStatus(unpaid.getId(), "COMPLETED", volunteerAuth.getId());

        // Calculate earnings
        VolunteerEarningsResponse earnings = taskService.getVolunteerEarnings(volunteerAuth.getId());

        assertNotNull(earnings);
        assertEquals(0, new BigDecimal("850.00").compareTo(earnings.getTotalEarnings())); // 500 + 350
        assertEquals(0, new BigDecimal("200.00").compareTo(earnings.getPendingEarnings())); // 200
        assertEquals(0, new BigDecimal("150.00").compareTo(earnings.getDisputedEarnings())); // 150
        assertEquals(2, earnings.getPaidTasksCount());
        assertEquals(1, earnings.getPendingTasksCount());
        assertEquals(1, earnings.getDisputedTasksCount());
        assertEquals(4, earnings.getAllPaidTasks().size());
    }

    @Test
    @DisplayName("15. shouldPersistPaymentStatusAcrossMultipleFetches")
    void shouldPersistPaymentStatusAcrossMultipleFetches() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("800.00"));
        taskService.confirmPayment(task.getId(), ngoAuth.getId());

        // Fetch as volunteer
        List<VolunteerTaskResponse> volTasks = taskService.getTasksForVolunteer(volunteerAuth.getId());
        VolunteerTaskResponse fetchedForVol = volTasks.stream().filter(t -> t.getId().equals(task.getId())).findFirst().orElseThrow();
        assertEquals("PAID", fetchedForVol.getPaymentStatus());
        assertEquals(new BigDecimal("800.00"), fetchedForVol.getPaymentAmount());
        assertNotNull(fetchedForVol.getPaidAt());

        // Fetch as assigner
        List<VolunteerTaskResponse> assignerTasks = taskService.getTasksAssignedByUser(ngoAuth.getId());
        VolunteerTaskResponse fetchedForAssigner = assignerTasks.stream().filter(t -> t.getId().equals(task.getId())).findFirst().orElseThrow();
        assertEquals("PAID", fetchedForAssigner.getPaymentStatus());
        assertEquals(new BigDecimal("800.00"), fetchedForAssigner.getPaymentAmount());
        assertNotNull(fetchedForAssigner.getPaidAt());
    }

    @Test
    @DisplayName("16. shouldPreventChangingTaskStatusAwayFromCompletedOncePaid")
    void shouldPreventChangingTaskStatusAwayFromCompletedOncePaid() {
        VolunteerTaskResponse task = createAndCompletePaidTask(new BigDecimal("250.00"));
        taskService.confirmPayment(task.getId(), ngoAuth.getId());

        // Attempting to reset completed status of a PAID task must fail
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                taskService.updateTaskStatus(task.getId(), "IN_PROGRESS", volunteerAuth.getId())
        );
        assertTrue(ex.getMessage().contains("whose payment has already been confirmed as PAID"));
    }

    private VolunteerTaskResponse createAndCompletePaidTask(BigDecimal amount) {
        VolunteerTaskRequest req = new VolunteerTaskRequest();
        req.setVolunteerId(volunteer.getId());
        req.setTaskType("PICKUP_DONATION");
        req.setTitle("Paid Task " + System.currentTimeMillis());
        req.setPickupAddress("Pickup Spot");
        req.setDestinationAddress("Drop Spot");
        req.setIsPaid(true);
        req.setPaymentAmount(amount);
        req.setCurrency("INR");
        req.setPaymentTerms("Immediate payment upon completion");

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), req);
        return taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteerAuth.getId());
    }
}
