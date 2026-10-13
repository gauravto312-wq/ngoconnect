package com.ngoconnect.service;

import com.ngoconnect.dto.AuthRequest;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthServiceTests {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Phase 4: Register Citizen - Successful")
    void testRegisterCitizenSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Citizen Test User");
        req.setEmail("citizen.test@ngoconnect.org");
        req.setPhone("9876543210");
        req.setPassword("Password123");
        req.setRole("CITIZEN");
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertEquals("CITIZEN", resp.getRole());
        assertEquals("citizen.test@ngoconnect.org", resp.getEmail());
    }

    @Test
    @DisplayName("Phase 4 & 9: Public Admin Registration - Blocked")
    void testPublicAdminRegistrationBlocked() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Malicious Admin");
        req.setEmail("fakeadmin@ngoconnect.org");
        req.setPhone("9876543210");
        req.setPassword("Password123");
        req.setRole("ADMIN");

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("Phase 4: Duplicate Email Registration - Rejected")
    void testDuplicateEmailRegistration() {
        RegisterRequest req = new RegisterRequest();
        req.setName("User 1");
        req.setEmail("duplicate@test.com");
        req.setPhone("9876543210");
        req.setPassword("Password123");
        req.setRole("CITIZEN");

        authService.register(req);

        RegisterRequest req2 = new RegisterRequest();
        req2.setName("User 2");
        req2.setEmail("duplicate@test.com");
        req2.setPhone("9876543211");
        req2.setPassword("Password123");
        req2.setRole("CITIZEN");

        assertThrows(BadRequestException.class, () -> authService.register(req2));
    }

    @Test
    @DisplayName("Phase 4: Login with Valid Credentials - Successful")
    void testLoginSuccess() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Login Test User");
        reg.setEmail("logintest@ngoconnect.org");
        reg.setPhone("9876543210");
        reg.setPassword("Password123");
        reg.setRole("VOLUNTEER");
        reg.setCity("Lucknow");
        reg.setAddress("Gomti Nagar");
        authService.register(reg);

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("logintest@ngoconnect.org");
        loginReq.setPassword("Password123");

        AuthResponse resp = authService.login(loginReq);

        assertNotNull(resp);
        assertNotNull(resp.getToken());
        assertEquals("VOLUNTEER", resp.getRole());
    }

    @Test
    @DisplayName("Phase 4: Login with Invalid Password - Rejected")
    void testLoginInvalidPassword() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Login Test User 2");
        reg.setEmail("logintest2@ngoconnect.org");
        reg.setPhone("9876543210");
        reg.setPassword("CorrectPassword123");
        reg.setRole("CITIZEN");
        authService.register(reg);

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("logintest2@ngoconnect.org");
        loginReq.setPassword("WrongPassword");

        assertThrows(Exception.class, () -> authService.login(loginReq));
    }

    @Test
    @DisplayName("Phase 4 & 5: Non-Admin Accessing Admin Login Portal - Rejected")
    void testNonAdminAdminLoginRejected() {
        RegisterRequest reg = new RegisterRequest();
        reg.setName("Normal Citizen");
        reg.setEmail("citizen.adminlogin@test.com");
        reg.setPhone("9876543210");
        reg.setPassword("Password123");
        reg.setRole("CITIZEN");
        authService.register(reg);

        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail("citizen.adminlogin@test.com");
        loginReq.setPassword("Password123");

        assertThrows(UnauthorizedException.class, () -> authService.adminLogin(loginReq));
    }
}
