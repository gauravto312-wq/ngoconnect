package com.ngoconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Role is required")
    private String role; // CITIZEN, VOLUNTEER, NGO, RESTAURANT (ADMIN NOT ALLOWED)

    // Role Specific Metadata
    private String address;
    private String city;
    private String state;

    // NGO Specific
    private String ngoName;
    private String description;
    private String cause;

    // Restaurant Specific
    private String restaurantName;
}
