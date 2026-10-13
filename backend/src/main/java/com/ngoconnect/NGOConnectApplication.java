package com.ngoconnect;

import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Slf4j
@SpringBootApplication
public class NGOConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(NGOConnectApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public CommandLineRunner seedRolesAndAdminUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        return args -> bootstrapRolesAndAdmin(userRepository, roleRepository, passwordEncoder, environment);
    }

    public void bootstrapRolesAndAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        String[] roles = {"CITIZEN", "VOLUNTEER", "NGO", "RESTAURANT", "ADMIN"};
        for (String roleName : roles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
            }
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        List<User> existingAdmins = userRepository.findByRoleName("ADMIN");
        if (!existingAdmins.isEmpty()) {
            log.info("Administrator account already exists in database. Skipping initial admin provisioning.");
            return;
        }

        String adminEmail = environment.getProperty("INITIAL_ADMIN_EMAIL", environment.getProperty("ADMIN_EMAIL", "")).trim();
        String adminPassword = environment.getProperty("INITIAL_ADMIN_PASSWORD", environment.getProperty("ADMIN_PASSWORD", "")).trim();
        String adminName = environment.getProperty("INITIAL_ADMIN_NAME", environment.getProperty("ADMIN_NAME", "System Administrator")).trim();
        String adminPhone = environment.getProperty("INITIAL_ADMIN_PHONE", environment.getProperty("ADMIN_PHONE", "9999999999")).trim();

        if (adminEmail.isEmpty() || adminPassword.isEmpty()) {
            log.warn("No administrator account detected in database and no INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_PASSWORD configured. Skipping initial admin creation.");
            return;
        }

        if (adminPassword.length() < 8) {
            log.error("Initial administrator password is too short (minimum 8 characters required). Skipping admin creation.");
            return;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.error("Cannot provision initial administrator: A user already exists with email '{}'. Aborting admin bootstrap to prevent modifying existing account.", adminEmail);
            return;
        }

        User admin = User.builder()
                .name(adminName.isEmpty() ? "System Administrator" : adminName)
                .email(adminEmail)
                .phone(adminPhone.isEmpty() ? "9999999999" : adminPhone)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(adminRole)
                .isActive(true)
                .build();
        userRepository.save(admin);
        log.info("Successfully provisioned initial administrator account for email: {}", adminEmail);
    }
}
