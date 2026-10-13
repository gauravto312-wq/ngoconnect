package com.ngoconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoconnect.dto.AuthRequest;
import com.ngoconnect.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthSecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("shouldRegisterCitizenSuccessfully - POST /api/v1/auth/register")
    void shouldRegisterCitizenSuccessfully() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Jane Citizen");
        req.setEmail("jane.citizen@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password123!");
        req.setRole("CITIZEN");
        req.setCity("Lucknow");
        req.setAddress("Hazratganj Main St");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andExpect(jsonPath("$.email").value("jane.citizen@example.com"));
    }

    @Test
    @DisplayName("shouldRejectDuplicateEmail - Return 400 Bad Request")
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest req1 = new RegisterRequest();
        req1.setName("User One");
        req1.setEmail("duplicate.user@example.com");
        req1.setPhone("9876543210");
        req1.setPassword("Password123!");
        req1.setRole("CITIZEN");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        RegisterRequest req2 = new RegisterRequest();
        req2.setName("User Two");
        req2.setEmail("duplicate.user@example.com");
        req2.setPhone("9876543211");
        req2.setPassword("Password123!");
        req2.setRole("CITIZEN");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered."));
    }

    @Test
    @DisplayName("shouldRejectAdminRegistrationViaPublicEndpoint - Block ADMIN role signup")
    void shouldRejectAdminRegistrationViaPublicEndpoint() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Fake Admin");
        req.setEmail("fakeadmin.signup@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password123!");
        req.setRole("ADMIN");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ADMIN registration is not allowed through public signup."));
    }

    @Test
    @DisplayName("shouldLoginWithValidCredentials - Return JWT Token")
    void shouldLoginWithValidCredentials() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Login User");
        req.setEmail("login.valid@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password123!");
        req.setRole("CITIZEN");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("login.valid@example.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("login.valid@example.com"));
    }

    @Test
    @DisplayName("shouldRejectInvalidPassword - Return 401 Unauthorized")
    void shouldRejectInvalidPassword() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Login User");
        req.setEmail("login.invalidpass@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password123!");
        req.setRole("CITIZEN");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("login.invalidpass@example.com");
        loginReq.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("shouldRejectNonAdminAtAdminLoginPortal - Return 401")
    void shouldRejectNonAdminAtAdminLoginPortal() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Standard Citizen");
        req.setEmail("citizen.adminportal@example.com");
        req.setPhone("9876543210");
        req.setPassword("Password123!");
        req.setRole("CITIZEN");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("citizen.adminportal@example.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied: Not an administrator account."));
    }

    @Test
    @DisplayName("shouldRejectRegistrationWithMissingRequiredFields - Validation 400")
    void shouldRejectRegistrationWithMissingRequiredFields() throws Exception {
        RegisterRequest req = new RegisterRequest();
        // Missing name, email, phone, password, role

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }
}
