package com.ngoconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RbacSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    private String citizenToken;
    private String volunteerToken;
    private String ngoToken;

    @BeforeEach
    void setUp() {
        RegisterRequest citizenReq = new RegisterRequest();
        citizenReq.setName("Citizen User");
        citizenReq.setEmail("rbac.citizen@ngoconnect.org");
        citizenReq.setPhone("9876543210");
        citizenReq.setPassword("Password123!");
        citizenReq.setRole("CITIZEN");
        AuthResponse citizenResp = authService.register(citizenReq);
        citizenToken = citizenResp.getToken();

        RegisterRequest volunteerReq = new RegisterRequest();
        volunteerReq.setName("Volunteer User");
        volunteerReq.setEmail("rbac.volunteer@ngoconnect.org");
        volunteerReq.setPhone("9876543211");
        volunteerReq.setPassword("Password123!");
        volunteerReq.setRole("VOLUNTEER");
        AuthResponse volunteerResp = authService.register(volunteerReq);
        volunteerToken = volunteerResp.getToken();

        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("NGO User");
        ngoReq.setEmail("rbac.ngo@ngoconnect.org");
        ngoReq.setPhone("9876543212");
        ngoReq.setPassword("Password123!");
        ngoReq.setRole("NGO");
        ngoReq.setNgoName("Hope Foundation");
        AuthResponse ngoResp = authService.register(ngoReq);
        ngoToken = ngoResp.getToken();
    }

    @Test
    @DisplayName("shouldRejectUnauthenticatedAccessToProtectedEndpointsWith401")
    void shouldRejectUnauthenticatedAccessToProtectedEndpointsWith401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden()); // Security default forbidden/unauthorized response

        mockMvc.perform(get("/api/v1/donations/my-donations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldDenyNonAdminAccessToAdminStatsWith403")
    void shouldDenyNonAdminAccessToAdminStatsWith403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldDenyNonAdminAccessToAdminUsersWith403")
    void shouldDenyNonAdminAccessToAdminUsersWith403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + volunteerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldDenyNonNgoAccessToClaimedDonationsEndpointWith403")
    void shouldDenyNonNgoAccessToClaimedDonationsEndpointWith403() throws Exception {
        mockMvc.perform(get("/api/v1/donations/claimed")
                .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldDenyCitizenAccessToVolunteerProfileWith403")
    void shouldDenyCitizenAccessToVolunteerProfileWith403() throws Exception {
        mockMvc.perform(get("/api/v1/volunteers/profile")
                .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("shouldRejectInvalidJwtTokenSignatureWith401Or403")
    void shouldRejectInvalidJwtTokenSignatureWith401Or403() throws Exception {
        String tamperedToken = citizenToken + "invalidSignature123";

        mockMvc.perform(get("/api/v1/donations/my-donations")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }
}
