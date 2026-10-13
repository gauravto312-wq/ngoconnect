package com.ngoconnect.admin;

import com.ngoconnect.NGOConnectApplication;
import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class AdminBootstrapSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NGOConnectApplication app;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify that all five application roles are created when missing")
    void testSeedsAllFiveRoles() {
        MockEnvironment env = new MockEnvironment();
        app.bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, env);

        assertTrue(roleRepository.findByName("CITIZEN").isPresent());
        assertTrue(roleRepository.findByName("VOLUNTEER").isPresent());
        assertTrue(roleRepository.findByName("NGO").isPresent());
        assertTrue(roleRepository.findByName("RESTAURANT").isPresent());
        assertTrue(roleRepository.findByName("ADMIN").isPresent());
    }

    @Test
    @DisplayName("Verify that application startup NEVER modifies or resets an existing admin's password hash")
    void testPreservesExistingAdminPasswordHash() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").build()));

        String existingHash = "$2a$10$PreExistingPasswordHashThatMustNeverBeModified12345678";
        User existingAdmin = User.builder()
                .name("Existing Super Admin")
                .email("admin@ngoconnect.org")
                .phone("9876543210")
                .passwordHash(existingHash)
                .role(adminRole)
                .isActive(true)
                .build();
        userRepository.save(existingAdmin);

        // Run bootstrap with environment variables that attempt to set another password
        MockEnvironment env = new MockEnvironment();
        env.setProperty("INITIAL_ADMIN_EMAIL", "admin@ngoconnect.org");
        env.setProperty("INITIAL_ADMIN_PASSWORD", "SomeDifferentPassword123!");

        app.bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, env);

        User postBootstrapAdmin = userRepository.findByEmail("admin@ngoconnect.org").orElseThrow();
        assertEquals(existingHash, postBootstrapAdmin.getPasswordHash(), "Existing admin password hash must remain completely unchanged");
        assertEquals("Existing Super Admin", postBootstrapAdmin.getName());
        assertEquals(adminRole.getId(), postBootstrapAdmin.getRole().getId());
        assertTrue(postBootstrapAdmin.getIsActive());
    }

    @Test
    @DisplayName("Verify that missing bootstrap credentials do not create an unsafe default admin")
    void testDoesNotCreateAdminWhenCredentialsMissing() {
        MockEnvironment env = new MockEnvironment();
        // No INITIAL_ADMIN_EMAIL or INITIAL_ADMIN_PASSWORD provided

        app.bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, env);

        List<User> admins = userRepository.findByRoleName("ADMIN");
        assertTrue(admins.isEmpty(), "No admin account should be created when environment credentials are not provided");
    }

    @Test
    @DisplayName("Verify that fresh database can be initialized securely with environment credentials")
    void testProvisionsInitialAdminWhenConfiguredOnFreshDatabase() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("INITIAL_ADMIN_EMAIL", "newadmin@ngoconnect.org");
        env.setProperty("INITIAL_ADMIN_PASSWORD", "SuperSecurePassword123!");
        env.setProperty("INITIAL_ADMIN_NAME", "Platform Administrator");
        env.setProperty("INITIAL_ADMIN_PHONE", "8888888888");

        app.bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, env);

        User createdAdmin = userRepository.findByEmail("newadmin@ngoconnect.org").orElseThrow();
        assertEquals("Platform Administrator", createdAdmin.getName());
        assertEquals("ADMIN", createdAdmin.getRole().getName());
        assertTrue(createdAdmin.getIsActive());
        assertTrue(passwordEncoder.matches("SuperSecurePassword123!", createdAdmin.getPasswordHash()));
    }

    @Test
    @DisplayName("Verify that an existing non-admin user is not accidentally promoted or overwritten")
    void testDoesNotOverwriteExistingNonAdminUser() {
        Role citizenRole = roleRepository.findByName("CITIZEN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("CITIZEN").build()));

        String citizenHash = passwordEncoder.encode("CitizenPassword123!");
        User citizen = User.builder()
                .name("Regular Citizen")
                .email("citizen@ngoconnect.org")
                .phone("9876543211")
                .passwordHash(citizenHash)
                .role(citizenRole)
                .isActive(true)
                .build();
        userRepository.save(citizen);

        MockEnvironment env = new MockEnvironment();
        env.setProperty("INITIAL_ADMIN_EMAIL", "citizen@ngoconnect.org");
        env.setProperty("INITIAL_ADMIN_PASSWORD", "NewAdminPassword123!");

        app.bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, env);

        User postUser = userRepository.findByEmail("citizen@ngoconnect.org").orElseThrow();
        assertEquals("CITIZEN", postUser.getRole().getName(), "User role must NOT be changed to ADMIN");
        assertEquals(citizenHash, postUser.getPasswordHash(), "User password must NOT be changed");
    }

    @Test
    @DisplayName("Verify that passwords shorter than 8 characters are rejected")
    void testRejectsShortPassword() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("INITIAL_ADMIN_EMAIL", "short@ngoconnect.org");
        env.setProperty("INITIAL_ADMIN_PASSWORD", "short");

        app.bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, env);

        assertTrue(userRepository.findByEmail("short@ngoconnect.org").isEmpty());
    }
}
