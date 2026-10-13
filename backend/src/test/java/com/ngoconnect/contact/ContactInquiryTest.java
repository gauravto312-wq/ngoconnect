package com.ngoconnect.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.ContactInquiryRequest;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.entity.ContactInquiry;
import com.ngoconnect.entity.Notification;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.ContactInquiryRepository;
import com.ngoconnect.repository.NotificationRepository;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ContactInquiryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private ContactInquiryRepository contactInquiryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private AuthResponse adminAuth;
    private AuthResponse citizenAuth;

    @BeforeEach
    void setUp() {
        contactInquiryRepository.deleteAll();
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        adminUser = User.builder()
                .name("System Admin")
                .email("admin@ngoconnect.org")
                .phone("9999999999")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(adminRole)
                .isActive(true)
                .build();
        adminUser = userRepository.save(adminUser);

        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setName("System Admin");
        adminReq.setEmail("admin@ngoconnect.org");
        adminReq.setPassword("Password123!");
        adminReq.setRole("ADMIN");

        RegisterRequest citizenReq = new RegisterRequest();
        citizenReq.setName("Regular Citizen");
        citizenReq.setEmail("citizen.contact@ngoconnect.org");
        citizenReq.setPhone("9876543210");
        citizenReq.setPassword("Password123!");
        citizenReq.setRole("CITIZEN");

        com.ngoconnect.dto.AuthRequest adminLoginReq = new com.ngoconnect.dto.AuthRequest();
        adminLoginReq.setEmail("admin@ngoconnect.org");
        adminLoginReq.setPassword("Password123!");
        adminAuth = authService.login(adminLoginReq);
        citizenAuth = authService.register(citizenReq);
    }

    @Test
    void testValidContactSubmission_PersistsAndCreatesAdminNotificationWithFullDetails() throws Exception {
        ContactInquiryRequest request = new ContactInquiryRequest();
        request.setName("Amritanshu Shukla");
        request.setEmail("amritanshu@example.com");
        request.setMessage("I want to know how NGOs can claim restaurant food availability.");

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<ContactInquiry> inquiries = contactInquiryRepository.findAll();
        assertEquals(1, inquiries.size());
        ContactInquiry inquiry = inquiries.get(0);
        assertEquals("Amritanshu Shukla", inquiry.getName());
        assertEquals("amritanshu@example.com", inquiry.getEmail());
        assertEquals("I want to know how NGOs can claim restaurant food availability.", inquiry.getMessage());
        assertEquals("NEW", inquiry.getStatus());

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(adminUser.getId());
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertTrue(notification.getTitle().contains("Amritanshu Shukla"));
        assertTrue(notification.getMessage().contains("Amritanshu Shukla"));
        assertTrue(notification.getMessage().contains("amritanshu@example.com"));
        assertTrue(notification.getMessage().contains("I want to know how NGOs can claim restaurant food availability."));
    }

    @Test
    void testContactSubmission_MissingName_ReturnsBadRequest() throws Exception {
        ContactInquiryRequest request = new ContactInquiryRequest();
        request.setName("");
        request.setEmail("valid@example.com");
        request.setMessage("Hello team!");

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testContactSubmission_InvalidEmail_ReturnsBadRequest() throws Exception {
        ContactInquiryRequest request = new ContactInquiryRequest();
        request.setName("John Doe");
        request.setEmail("not-an-email");
        request.setMessage("Hello team!");

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testContactSubmission_MissingMessage_ReturnsBadRequest() throws Exception {
        ContactInquiryRequest request = new ContactInquiryRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setMessage("   ");

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testContactSubmission_ExcessivelyLongMessage_ReturnsBadRequest() throws Exception {
        ContactInquiryRequest request = new ContactInquiryRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setMessage("A".repeat(2005));

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAdminGetInquiries_AuthorizedForAdmin_Success() throws Exception {
        ContactInquiry inquiry = ContactInquiry.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .message("Need help with volunteer registration.")
                .status("NEW")
                .build();
        contactInquiryRepository.save(inquiry);

        mockMvc.perform(get("/api/v1/contact/admin")
                        .header("Authorization", "Bearer " + adminAuth.getToken()))
                .andExpect(status().isOk());
    }

    @Test
    void testNonAdminGetInquiries_ReturnsForbidden403() throws Exception {
        mockMvc.perform(get("/api/v1/contact/admin")
                        .header("Authorization", "Bearer " + citizenAuth.getToken()))
                .andExpect(status().isForbidden());
    }
}
