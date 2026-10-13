package com.ngoconnect.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoconnect.NGOConnectApplication;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.dto.VolunteerTaskRequest;
import com.ngoconnect.dto.VolunteerTaskResponse;
import com.ngoconnect.entity.ContactInquiry;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.entity.Volunteer;
import com.ngoconnect.repository.ContactInquiryRepository;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.repository.VolunteerRepository;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NGOConnectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminDisputeAndContactWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private VolunteerRepository volunteerRepository;

    @Autowired
    private ContactInquiryRepository contactInquiryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String citizenToken;
    private AuthResponse ngoAuth;
    private AuthResponse volunteerAuth;
    private Volunteer volunteer;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        // Create Admin user
        User adminUser = User.builder()
                .name("Super Admin")
                .email("super.admin@ngoconnect.org")
                .phone("9999900000")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(adminRole)
                .isActive(true)
                .build();
        userRepository.save(adminUser);

        // Authenticate admin
        com.ngoconnect.dto.AuthRequest loginReq = new com.ngoconnect.dto.AuthRequest();
        loginReq.setEmail("super.admin@ngoconnect.org");
        loginReq.setPassword("AdminPass123!");
        adminToken = authService.login(loginReq).getToken();

        // Create Citizen
        RegisterRequest citizenReq = new RegisterRequest();
        citizenReq.setName("Regular Citizen");
        citizenReq.setEmail("regular.citizen@ngoconnect.org");
        citizenReq.setPhone("9888800001");
        citizenReq.setPassword("Password123!");
        citizenReq.setRole("CITIZEN");
        citizenToken = authService.register(citizenReq).getToken();

        // Create NGO
        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("Seva Trust NGO");
        ngoReq.setEmail("seva.trust@ngoconnect.org");
        ngoReq.setPhone("9888800002");
        ngoReq.setPassword("Password123!");
        ngoReq.setRole("NGO");
        ngoReq.setNgoName("Seva Trust");
        ngoAuth = authService.register(ngoReq);

        // Create Volunteer
        RegisterRequest volReq = new RegisterRequest();
        volReq.setName("Vikram Volunteer");
        volReq.setEmail("vikram.volunteer@ngoconnect.org");
        volReq.setPhone("9888800003");
        volReq.setPassword("Password123!");
        volReq.setRole("VOLUNTEER");
        volReq.setCity("Delhi");
        volunteerAuth = authService.register(volReq);

        volunteer = volunteerRepository.findByUserId(volunteerAuth.getId()).orElseThrow();
    }

    @Test
    @DisplayName("shouldRetrieveDisputedTasksForAdminAndDenyCitizen")
    void shouldRetrieveDisputedTasksForAdminAndDenyCitizen() throws Exception {
        // Create and complete a paid task
        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setTaskType("FOOD_DRIVE_HELP");
        taskReq.setTitle("Community Food Distribution");
        taskReq.setPickupAddress("Warehouse 1");
        taskReq.setDestinationAddress("Shelter Home");
        taskReq.setIsPaid(true);
        taskReq.setPaymentAmount(new BigDecimal("500.00"));
        taskReq.setCurrency("INR");
        taskReq.setPaymentTerms("Post delivery");

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), taskReq);
        taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteerAuth.getId());
        taskService.disputePayment(task.getId(), volunteerAuth.getId(), "Payment delay after task handover");

        // Citizen accessing disputed tasks must be 403 Forbidden
        mockMvc.perform(get("/api/v1/tasks/disputed")
                .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // Admin accessing disputed tasks must be 200 OK and return the task
        mockMvc.perform(get("/api/v1/tasks/disputed")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].paymentStatus").value("DISPUTED"))
                .andExpect(jsonPath("$[0].disputeReason").value("Payment delay after task handover"));
    }

    @Test
    @DisplayName("shouldAllowAdminToResolveDisputeWithPaidStatus")
    void shouldAllowAdminToResolveDisputeWithPaidStatus() throws Exception {
        VolunteerTaskRequest taskReq = new VolunteerTaskRequest();
        taskReq.setVolunteerId(volunteer.getId());
        taskReq.setTaskType("PICKUP_DONATION");
        taskReq.setTitle("Emergency Bread Delivery");
        taskReq.setPickupAddress("Bakery");
        taskReq.setDestinationAddress("Orphanage");
        taskReq.setIsPaid(true);
        taskReq.setPaymentAmount(new BigDecimal("350.00"));
        taskReq.setCurrency("INR");

        VolunteerTaskResponse task = taskService.assignTask(ngoAuth.getId(), taskReq);
        taskService.updateTaskStatus(task.getId(), "COMPLETED", volunteerAuth.getId());
        taskService.disputePayment(task.getId(), ngoAuth.getId(), "Quality check pending");

        // Admin resolves dispute
        Map<String, String> resolutionBody = Map.of(
                "resolution", "PAID",
                "remarks", "Verified task proof; releasing full payment to volunteer."
        );

        mockMvc.perform(post("/api/v1/tasks/" + task.getId() + "/resolve-dispute")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resolutionBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }

    @Test
    @DisplayName("shouldRetrieveContactInquiriesForAdmin")
    void shouldRetrieveContactInquiriesForAdmin() throws Exception {
        contactInquiryRepository.save(ContactInquiry.builder()
                .name("Rohit Sharma")
                .email("rohit@corporate.org")
                .message("We would like to sponsor food recovery in North Delhi.")
                .status("NEW")
                .build());

        mockMvc.perform(get("/api/v1/contact/admin")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name").value("Rohit Sharma"))
                .andExpect(jsonPath("$[0].email").value("rohit@corporate.org"));
    }

    @Test
    @DisplayName("shouldRetrieveAllRegisteredNgosForAdmin")
    void shouldRetrieveAllRegisteredNgosForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ngos")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].ngoName").isNotEmpty());
    }
}
