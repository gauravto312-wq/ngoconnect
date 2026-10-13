package com.ngoconnect.service.impl;

import com.ngoconnect.dto.AuthRequest;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.entity.*;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.exception.UnauthorizedException;
import com.ngoconnect.repository.*;
import com.ngoconnect.security.JwtTokenProvider;
import com.ngoconnect.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NgoRepository ngoRepository;
    private final RestaurantRepository restaurantRepository;
    private final VolunteerRepository volunteerRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String roleName = request.getRole().toUpperCase();
        if ("ADMIN".equals(roleName)) {
            throw new BadRequestException("ADMIN registration is not allowed through public signup.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered.");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Location creation if city & address provided
        Location location = null;
        if (request.getCity() != null && request.getAddress() != null) {
            location = Location.builder()
                    .address(request.getAddress())
                    .city(request.getCity())
                    .state(request.getState() != null ? request.getState() : "State")
                    .build();
            location = locationRepository.save(location);
        }

        // Role specific profile creation
        if ("NGO".equals(roleName)) {
            Ngo ngo = Ngo.builder()
                    .user(user)
                    .ngoName(request.getNgoName() != null ? request.getNgoName() : request.getName())
                    .description(request.getDescription())
                    .cause(request.getCause())
                    .contactNumber(request.getPhone())
                    .address(request.getAddress() != null ? request.getAddress() : "Address")
                    .city(request.getCity() != null ? request.getCity() : "City")
                    .state(request.getState() != null ? request.getState() : "State")
                    .location(location)
                    .verificationStatus("PENDING")
                    .build();
            ngoRepository.save(ngo);
        } else if ("RESTAURANT".equals(roleName)) {
            Restaurant restaurant = Restaurant.builder()
                    .user(user)
                    .restaurantName(request.getRestaurantName() != null ? request.getRestaurantName() : request.getName())
                    .description(request.getDescription())
                    .contactNumber(request.getPhone())
                    .address(request.getAddress() != null ? request.getAddress() : "Address")
                    .city(request.getCity() != null ? request.getCity() : "City")
                    .state(request.getState() != null ? request.getState() : "State")
                    .location(location)
                    .verificationStatus("PENDING")
                    .build();
            restaurantRepository.save(restaurant);
        } else if ("VOLUNTEER".equals(roleName)) {
            String volunteerCode = "VOL-" + (10000 + new Random().nextInt(90000));
            Volunteer volunteer = Volunteer.builder()
                    .user(user)
                    .volunteerCode(volunteerCode)
                    .address(request.getAddress() != null ? request.getAddress() : "Address")
                    .city(request.getCity() != null ? request.getCity() : "City")
                    .state(request.getState() != null ? request.getState() : "State")
                    .location(location)
                    .availabilityStatus("ACTIVE")
                    .build();
            volunteerRepository.save(volunteer);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanEmail, request.getPassword())
            );

            User user = userRepository.findByEmail(cleanEmail)
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password credentials."));

            if (!user.getIsActive()) {
                throw new UnauthorizedException("Account is deactivated. Please contact support.");
            }

            String jwt = tokenProvider.generateToken(authentication);

            return AuthResponse.builder()
                    .token(jwt)
                    .tokenType("Bearer")
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().getName())
                    .build();
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new UnauthorizedException("Invalid email or password credentials.");
        }
    }

    @Override
    public AuthResponse adminLogin(AuthRequest request) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid admin credentials."));

        if (!"ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new UnauthorizedException("Access denied: Not an administrator account.");
        }

        return login(request);
    }
}
