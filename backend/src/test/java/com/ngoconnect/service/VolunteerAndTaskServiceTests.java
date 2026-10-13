package com.ngoconnect.service;

import com.ngoconnect.dto.*;
import com.ngoconnect.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class VolunteerAndTaskServiceTests {

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AuthService authService;

    private AuthResponse registerUser(String name, String email, String role, String city) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPhone("9876543210");
        req.setPassword("Password123");
        req.setRole(role);
        req.setAddress("Test Street");
        req.setCity(city);
        req.setState("Uttar Pradesh");
        req.setNgoName(role.equals("NGO") ? name : null);
        return authService.register(req);
    }

    @Test
    @DisplayName("Phase 7 & 14: Volunteer Unique Code & Active/Inactive Toggle")
    void testVolunteerProfileAndToggle() {
        AuthResponse volUser = registerUser("Amit Volunteer", "amit.vol@test.com", "VOLUNTEER", "Lucknow");

        VolunteerProfileResponse prof = volunteerService.getVolunteerProfile(volUser.getId());
        assertNotNull(prof);
        assertNotNull(prof.getVolunteerCode());
        assertTrue(prof.getVolunteerCode().startsWith("VOL-"));
        assertEquals("ACTIVE", prof.getAvailabilityStatus());

        // Toggle to INACTIVE
        VolunteerStatusUpdateRequest req = new VolunteerStatusUpdateRequest();
        req.setAvailabilityStatus("INACTIVE");
        VolunteerProfileResponse updated = volunteerService.updateStatus(volUser.getId(), req);

        assertEquals("INACTIVE", updated.getAvailabilityStatus());
    }

    @Test
    @DisplayName("Phase 8 & 26: Active Volunteer Search Filter")
    void testActiveVolunteerSearch() {
        AuthResponse volActive = registerUser("Priya Active", "priya.active@test.com", "VOLUNTEER", "Lucknow");
        AuthResponse volInactive = registerUser("Rahul Inactive", "rahul.inactive@test.com", "VOLUNTEER", "Lucknow");

        // Set Rahul to INACTIVE
        VolunteerStatusUpdateRequest req = new VolunteerStatusUpdateRequest();
        req.setAvailabilityStatus("INACTIVE");
        volunteerService.updateStatus(volInactive.getId(), req);

        List<VolunteerProfileResponse> activeList = volunteerService.searchActiveVolunteers("Lucknow");

        boolean foundActive = activeList.stream().anyMatch(v -> v.getUserId().equals(volActive.getId()));
        boolean foundInactive = activeList.stream().anyMatch(v -> v.getUserId().equals(volInactive.getId()));

        assertTrue(foundActive, "ACTIVE volunteer MUST appear in active volunteer search.");
        assertFalse(foundInactive, "INACTIVE volunteer MUST NOT appear in active volunteer search!");
    }

    @Test
    @DisplayName("Phase 9 & 24: Task Assignment & State Machine Transitions")
    void testTaskAssignmentAndLifecycle() {
        AuthResponse ngo = registerUser("NGO Hope", "ngo.hope@test.com", "NGO", "Lucknow");
        AuthResponse volUser = registerUser("Suresh Vol", "suresh.vol@test.com", "VOLUNTEER", "Lucknow");

        VolunteerProfileResponse volProf = volunteerService.getVolunteerProfile(volUser.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volProf.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Pickup Surplus Meals");
        taskReq.setPickupAddress("Restaurant Gomti Nagar");
        taskReq.setDestinationAddress("NGO Hazratganj");
        taskReq.setScheduledTime(LocalDateTime.now().plusHours(2));

        VolunteerTaskResponse taskResp = taskService.assignTask(ngo.getId(), taskReq);
        assertNotNull(taskResp);
        assertEquals("ASSIGNED", taskResp.getStatus());

        // Volunteer accepts
        VolunteerTaskResponse accepted = taskService.updateTaskStatus(taskResp.getId(), "ACCEPTED", volUser.getId());
        assertEquals("ACCEPTED", accepted.getStatus());

        // Volunteer marks IN_PROGRESS
        VolunteerTaskResponse inProg = taskService.updateTaskStatus(taskResp.getId(), "IN_PROGRESS", volUser.getId());
        assertEquals("IN_PROGRESS", inProg.getStatus());

        // Volunteer completes
        VolunteerTaskResponse completed = taskService.updateTaskStatus(taskResp.getId(), "COMPLETED", volUser.getId());
        assertEquals("COMPLETED", completed.getStatus());
    }

    @Test
    @DisplayName("Phase 24: Task Assignment to INACTIVE Volunteer - Rejected")
    void testTaskAssignmentToInactiveVolunteerRejected() {
        AuthResponse ngo = registerUser("NGO Trust", "ngo.trust@test.com", "NGO", "Lucknow");
        AuthResponse volUser = registerUser("Karan Inactive", "karan.inact@test.com", "VOLUNTEER", "Lucknow");

        VolunteerStatusUpdateRequest toggleReq = new VolunteerStatusUpdateRequest();
        toggleReq.setAvailabilityStatus("INACTIVE");
        volunteerService.updateStatus(volUser.getId(), toggleReq);

        VolunteerProfileResponse volProf = volunteerService.getVolunteerProfile(volUser.getId());

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volProf.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Pickup Task");
        taskReq.setPickupAddress("Address 1");
        taskReq.setDestinationAddress("Address 2");
        taskReq.setScheduledTime(LocalDateTime.now().plusHours(1));

        assertThrows(BadRequestException.class, () -> taskService.assignTask(ngo.getId(), taskReq));
    }
}
