package com.ngoconnect;

import com.ngoconnect.entity.Role;
import com.ngoconnect.entity.User;
import com.ngoconnect.repository.RoleRepository;
import com.ngoconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class NGOConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(NGOConnectApplication.class, args);
    }

    @Bean
    @org.springframework.context.annotation.Profile("!test")
    public CommandLineRunner seedRolesAndAdminUser(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String[] roles = {"CITIZEN", "VOLUNTEER", "NGO", "RESTAURANT", "ADMIN"};
            for (String roleName : roles) {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    roleRepository.save(Role.builder().name(roleName).build());
                }
            }

            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

            userRepository.findByEmail("admin@ngoconnect.org").ifPresentOrElse(
                admin -> {
                    admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
                    admin.setRole(adminRole);
                    admin.setIsActive(true);
                    userRepository.save(admin);
                },
                () -> {
                    User admin = User.builder()
                            .name("System Administrator")
                            .email("admin@ngoconnect.org")
                            .phone("9999999999")
                            .passwordHash(passwordEncoder.encode("Admin@123456"))
                            .role(adminRole)
                            .isActive(true)
                            .build();
                    userRepository.save(admin);
                }
            );
        };
    }
}
