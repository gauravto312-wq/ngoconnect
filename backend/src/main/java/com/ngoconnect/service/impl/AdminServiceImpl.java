package com.ngoconnect.service.impl;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.*;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.*;
import com.ngoconnect.service.AdminService;
import com.ngoconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final NgoVerificationRepository ngoVerificationRepository;
    private final RestaurantRepository restaurantRepository;
    private final VolunteerRepository volunteerRepository;
    private final DonationRepository donationRepository;
    private final FoodDriveRepository foodDriveRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalCitizens = userRepository.findByRoleName("CITIZEN").size();
        long totalVolunteers = userRepository.findByRoleName("VOLUNTEER").size();
        long activeVolunteers = volunteerRepository.countByAvailabilityStatus("ACTIVE");
        long totalNgos = ngoRepository.count();
        long pendingNgoVerifs = ngoVerificationRepository.findByVerificationStatus("PENDING").size();
        long totalRestaurants = restaurantRepository.count();
        long pendingRestVerifs = restaurantRepository.findByVerificationStatus("PENDING").size();
        long totalDonations = donationRepository.count();
        long availableDonations = donationRepository.countByStatus("AVAILABLE");
        long claimedDonations = donationRepository.countByStatus("CLAIMED");
        long completedDonations = donationRepository.countByStatus("COMPLETED");
        long totalFoodDrives = foodDriveRepository.count();

        return AdminDashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalCitizens(totalCitizens)
                .totalVolunteers(totalVolunteers)
                .activeVolunteers(activeVolunteers)
                .totalNgos(totalNgos)
                .pendingNgoVerifications(pendingNgoVerifs)
                .totalRestaurants(totalRestaurants)
                .pendingRestaurantVerifications(pendingRestVerifs)
                .totalDonations(totalDonations)
                .availableDonations(availableDonations)
                .claimedDonations(claimedDonations)
                .completedDonations(completedDonations)
                .totalFoodDrives(totalFoodDrives)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long userId, Boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setIsActive(isActive);
        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NgoVerificationResponse> getPendingNgoVerifications() {
        return ngoVerificationRepository.findByVerificationStatus("PENDING").stream()
                .map(this::mapToVerifResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NgoVerificationResponse reviewNgoVerification(Long verificationId, Long adminId, String status, String remarks) {
        NgoVerification verif = ngoVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        String statusUpper = status.toUpperCase();
        verif.setVerificationStatus(statusUpper);
        verif.setAdmin(admin);
        verif.setRemarks(remarks);
        verif.setReviewedAt(LocalDateTime.now());
        verif = ngoVerificationRepository.save(verif);

        // Update overall NGO status
        Ngo ngo = verif.getNgo();
        ngo.setVerificationStatus(statusUpper);
        ngoRepository.save(ngo);

        // Notify NGO
        notificationService.sendNotification(
                ngo.getUser().getId(),
                "NGO Verification " + statusUpper,
                "Your verification request for NGO '" + ngo.getNgoName() + "' has been " + statusUpper + ". Remarks: " + (remarks != null ? remarks : "None"),
                "NGO_VERIFICATION",
                "NGO",
                ngo.getId()
        );

        return mapToVerifResponse(verif);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NgoProfileResponse> getAllNgos() {
        return ngoRepository.findAll().stream()
                .map(this::mapToNgoResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NgoProfileResponse reviewNgoStatus(Long ngoId, Long adminId, String status) {
        Ngo ngo = ngoRepository.findById(ngoId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO not found with id: " + ngoId));

        String statusUpper = status.toUpperCase();
        ngo.setVerificationStatus(statusUpper);
        ngo = ngoRepository.save(ngo);

        notificationService.sendNotification(
                ngo.getUser().getId(),
                "NGO Status Update",
                "Your NGO verification status is now " + statusUpper + ".",
                "NGO_VERIFICATION",
                "NGO",
                ngo.getId()
        );

        return mapToNgoResponse(ngo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantProfileResponse> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::mapToRestaurantResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RestaurantProfileResponse reviewRestaurantStatus(Long restaurantId, Long adminId, String status) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        String statusUpper = status.toUpperCase();
        restaurant.setVerificationStatus(statusUpper);
        restaurant = restaurantRepository.save(restaurant);

        notificationService.sendNotification(
                restaurant.getUser().getId(),
                "Restaurant Status Update",
                "Your restaurant verification status is now " + statusUpper + ".",
                "RESTAURANT_VERIFICATION",
                "RESTAURANT",
                restaurant.getId()
        );

        return mapToRestaurantResponse(restaurant);
    }

    private UserResponse mapToUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole().getName())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private NgoVerificationResponse mapToVerifResponse(NgoVerification v) {
        return NgoVerificationResponse.builder()
                .id(v.getId())
                .ngoId(v.getNgo().getId())
                .ngoName(v.getNgo().getNgoName())
                .documentType(v.getDocumentType())
                .documentUrl(v.getDocumentUrl())
                .documentNumber(v.getDocumentNumber())
                .verificationStatus(v.getVerificationStatus())
                .adminName(v.getAdmin() != null ? v.getAdmin().getName() : null)
                .remarks(v.getRemarks())
                .reviewedAt(v.getReviewedAt())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private NgoProfileResponse mapToNgoResponse(Ngo n) {
        String verifStatus = n.getVerificationStatus();
        if (verifStatus == null || verifStatus.isBlank()) {
            verifStatus = "PENDING";
        }
        return NgoProfileResponse.builder()
                .id(n.getId())
                .userId(n.getUser() != null ? n.getUser().getId() : null)
                .ngoName(n.getNgoName())
                .description(n.getDescription())
                .cause(n.getCause())
                .contactNumber(n.getContactNumber())
                .email(n.getUser() != null ? n.getUser().getEmail() : null)
                .phone(n.getUser() != null ? n.getUser().getPhone() : n.getContactNumber())
                .address(n.getAddress())
                .city(n.getCity())
                .state(n.getState())
                .profileImageUrl(n.getProfileImageUrl())
                .galleryPhotos(n.getGalleryPhotos())
                .verificationStatus(verifStatus)
                .createdAt(n.getCreatedAt())
                .build();
    }

    private RestaurantProfileResponse mapToRestaurantResponse(Restaurant r) {
        String verifStatus = r.getVerificationStatus();
        if (verifStatus == null || verifStatus.isBlank()) {
            verifStatus = "PENDING";
        }
        return RestaurantProfileResponse.builder()
                .id(r.getId())
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .restaurantName(r.getRestaurantName())
                .description(r.getDescription())
                .contactNumber(r.getContactNumber())
                .email(r.getUser() != null ? r.getUser().getEmail() : null)
                .phone(r.getUser() != null ? r.getUser().getPhone() : r.getContactNumber())
                .address(r.getAddress())
                .city(r.getCity())
                .state(r.getState())
                .profileImageUrl(r.getProfileImageUrl())
                .galleryPhotos(r.getGalleryPhotos())
                .verificationStatus(verifStatus)
                .createdAt(r.getCreatedAt())
                .build();
    }
}
