package com.ngoconnect.verification;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.Restaurant;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.RestaurantRepository;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.service.AdminService;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class RestaurantVerificationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private AuthResponse restaurantAuth;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        adminUser = userRepository.save(User.builder()
                .name("Super Admin")
                .email("admin.restaurantverif@ngoconnect.org")
                .phone("9999999999")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(adminRole)
                .isActive(true)
                .build());

        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("Tasty Eats Owner");
        regReq.setEmail("tasty.eats@ngoconnect.org");
        regReq.setPhone("9876543210");
        regReq.setPassword("Password123!");
        regReq.setRole("RESTAURANT");
        regReq.setRestaurantName("Tasty Eats Restaurant");
        regReq.setCity("Lucknow");
        regReq.setAddress("Mahanagar");

        restaurantAuth = authService.register(regReq);
    }

    @Test
    @DisplayName("shouldCreateRestaurantWithPendingVerificationStatus")
    void shouldCreateRestaurantWithPendingVerificationStatus() {
        RestaurantProfileResponse profile = restaurantService.getRestaurantProfile(restaurantAuth.getId());

        assertNotNull(profile);
        assertEquals("Tasty Eats Restaurant", profile.getRestaurantName());
        assertEquals("PENDING", profile.getVerificationStatus());
    }

    @Test
    @DisplayName("shouldAllowAdminToApproveRestaurant")
    void shouldAllowAdminToApproveRestaurant() {
        Restaurant rest = restaurantRepository.findByUserId(restaurantAuth.getId()).orElseThrow();

        RestaurantProfileResponse updated = adminService.reviewRestaurantStatus(rest.getId(), adminUser.getId(), "APPROVED");

        assertNotNull(updated);
        assertEquals("APPROVED", updated.getVerificationStatus());

        Restaurant dbRest = restaurantRepository.findById(rest.getId()).orElseThrow();
        assertEquals("APPROVED", dbRest.getVerificationStatus());
    }

    @Test
    @DisplayName("shouldAllowAdminToRejectRestaurant")
    void shouldAllowAdminToRejectRestaurant() {
        Restaurant rest = restaurantRepository.findByUserId(restaurantAuth.getId()).orElseThrow();

        RestaurantProfileResponse updated = adminService.reviewRestaurantStatus(rest.getId(), adminUser.getId(), "REJECTED");

        assertNotNull(updated);
        assertEquals("REJECTED", updated.getVerificationStatus());

        Restaurant dbRest = restaurantRepository.findById(rest.getId()).orElseThrow();
        assertEquals("REJECTED", dbRest.getVerificationStatus());
    }

    @Test
    @DisplayName("shouldReturnPublicRestaurantProfileWithAccurateDbStatus")
    void shouldReturnPublicRestaurantProfileWithAccurateDbStatus() {
        Restaurant rest = restaurantRepository.findByUserId(restaurantAuth.getId()).orElseThrow();

        RestaurantProfileResponse publicBefore = restaurantService.getPublicRestaurantById(rest.getId());
        assertEquals("PENDING", publicBefore.getVerificationStatus());

        adminService.reviewRestaurantStatus(rest.getId(), adminUser.getId(), "APPROVED");

        RestaurantProfileResponse publicAfter = restaurantService.getPublicRestaurantById(rest.getId());
        assertEquals("APPROVED", publicAfter.getVerificationStatus());
    }
}
