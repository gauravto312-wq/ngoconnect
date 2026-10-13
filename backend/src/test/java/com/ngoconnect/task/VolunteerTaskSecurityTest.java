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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class VolunteerTaskSecurityTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private VolunteerRepository volunteerRepository;

    private AuthResponse ngoAuth;
    private AuthResponse volunteer1Auth;
    private AuthResponse volunteer2Auth;
    private Volunteer vol1;
    private Volunteer vol2;

    @BeforeEach
    void setUp() {
        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("Helping Hands NGO");
        ngoReq.setEmail("ngo.tasks@ngoconnect.org");
        ngoReq.setPhone("9876543210");
        ngoReq.setPassword("Password123!");
        ngoReq.setRole("NGO");
        ngoReq.setNgoName("Helping Hands");
        ngoAuth = authService.register(ngoReq);

        RegisterRequest vol1Req = new RegisterRequest();
        vol1Req.setName("Volunteer Alex");
        vol1Req.setEmail("alex.vol1@ngoconnect.org");
        vol1Req.setPhone("9876543211");
        vol1Req.setPassword("Password123!");
        vol1Req.setRole("VOLUNTEER");
        vol1Req.setCity("Lucknow");
        volunteer1Auth = authService.register(vol1Req);

        RegisterRequest vol2Req = new RegisterRequest();
        vol2Req.setName("Volunteer Bob");
        vol2Req.setEmail("bob.vol2@ngoconnect.org");
        vol2Req.setPhone("9876543212");
        vol2Req.setPassword("Password123!");
        vol2Req.setRole("VOLUNTEER");
        vol2Req.setCity("Lucknow");
        volunteer2Auth = authService.register(vol2Req);

        vol1 = volunteerRepository.findByUserId(volunteer1Auth.getId()).orElseThrow();
        vol2 = volunteerRepository.findByUserId(volunteer2Auth.getId()).orElseThrow();
    }

    @Test
    @DisplayName("shouldAssignTaskToActiveVolunteer")
    void shouldAssignTaskToActiveVolunteer() {
        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(vol1.getId());
        taskReq.setTaskType("PICKUP");
        taskReq.setTitle("Pickup Surplus Meals from Bakehouse");
        taskReq.setDescription("Collect 30 meals and deliver to shelter");
        taskReq.setPickupAddress("Hazratganj");
        taskReq.setDestinationAddress("Aliganj Shelter");

        VolunteerTaskResponse taskResp = taskService.assignTask(ngoAuth.getId(), taskReq);

        assertNotNull(taskResp);
        assertNotNull(taskResp.getId());
        assertEquals("ASSIGNED", taskResp.getStatus());
        assertEquals(vol1.getId(), taskResp.getVolunteerId());
    }

    @Test
    @DisplayName("shouldRejectTaskAssignmentToInactiveVolunteer")
    void shouldRejectTaskAssignmentToInactiveVolunteer() {
        // Set volunteer 1 status to INACTIVE
        VolunteerStatusUpdateRequest statusReq = new VolunteerStatusUpdateRequest();
        statusReq.setAvailabilityStatus("INACTIVE");
        volunteerService.updateStatus(volunteer1Auth.getId(), statusReq);

        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(vol1.getId());
        taskReq.setTaskType("PICKUP");
        taskReq.setTitle("Pickup Clothes");
        taskReq.setPickupAddress("Hazratganj");

        assertThrows(BadRequestException.class, () -> taskService.assignTask(ngoAuth.getId(), taskReq));
    }

    @Test
    @DisplayName("shouldAllowAssignedVolunteerToUpdateTaskStatus")
    void shouldAllowAssignedVolunteerToUpdateTaskStatus() {
        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(vol1.getId());
        taskReq.setTaskType("DELIVERY");
        taskReq.setTitle("Deliver Food Packets");
        taskReq.setPickupAddress("Hazratganj");

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), taskReq);

        // Assigned volunteer (Alex) updates status to IN_PROGRESS
        VolunteerTaskResponse updated = taskService.updateTaskStatus(task.getId(), "IN_PROGRESS", volunteer1Auth.getId());
        assertEquals("IN_PROGRESS", updated.getStatus());

        // Assigned volunteer updates status to COMPLETED
        VolunteerTaskResponse completed = taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteer1Auth.getId());
        assertEquals("COMPLETED", completed.getStatus());
    }

    @Test
    @DisplayName("shouldPreventUnauthorizedUserFromUpdatingOtherUserTask - IDOR Protection Test")
    void shouldPreventUnauthorizedUserFromUpdatingOtherUserTask() {
        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(vol1.getId()); // Task assigned to Alex (vol1)
        taskReq.setTaskType("DELIVERY");
        taskReq.setTitle("Exclusive Task For Alex");
        taskReq.setPickupAddress("Hazratganj");

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), taskReq);

        // Bob (vol2) attempts to manipulate Alex's task ID -> EXPECT 403 FORBIDDEN
        assertThrows(ForbiddenException.class, () ->
                taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteer2Auth.getId())
        );
    }
}
