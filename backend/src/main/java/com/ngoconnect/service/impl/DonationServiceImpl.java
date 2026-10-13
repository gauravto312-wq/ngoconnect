package com.ngoconnect.service.impl;

import com.ngoconnect.dto.ClaimResponse;
import com.ngoconnect.dto.DonationRequest;
import com.ngoconnect.dto.DonationResponse;
import com.ngoconnect.entity.*;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ConflictException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.*;
import com.ngoconnect.service.DonationService;
import com.ngoconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final DonationClaimRepository donationClaimRepository;
    private final DonationStatusHistoryRepository donationStatusHistoryRepository;
    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final RestaurantBranchRepository branchRepository;
    private final LocationRepository locationRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodAvailabilityRepository foodAvailabilityRepository;
    private final NotificationService notificationService;
    private final VolunteerTaskRepository volunteerTaskRepository;

    @Override
    @Transactional
    public DonationResponse createDonation(Long userId, DonationRequest request) {
        User donorUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RestaurantBranch branch = null;
        if (request.getRestaurantBranchId() != null) {
            branch = branchRepository.findById(request.getRestaurantBranchId())
                    .orElse(null);
        }

        Location location = Location.builder()
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .build();
        location = locationRepository.save(location);

        String pickupCode = "NGC-" + (100000 + new Random().nextInt(900000));

        Donation donation = Donation.builder()
                .donorUser(donorUser)
                .restaurantBranch(branch)
                .category(request.getCategory().toUpperCase())
                .title(request.getTitle())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null ? request.getUnit() : "packets")
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .location(location)
                .availabilityStart(request.getAvailabilityStart() != null ? request.getAvailabilityStart() : LocalDateTime.now())
                .availabilityEnd(request.getAvailabilityEnd() != null ? request.getAvailabilityEnd() : LocalDateTime.now().plusHours(24))
                .pickupCode(pickupCode)
                .status("AVAILABLE")
                .build();

        donation = donationRepository.save(donation);

        // Record Status History
        DonationStatusHistory history = DonationStatusHistory.builder()
                .donation(donation)
                .status("AVAILABLE")
                .changedByUser(donorUser)
                .remarks("Donation published to marketplace")
                .build();
        donationStatusHistoryRepository.save(history);

        // Notify nearby NGOs registered in the same city
        List<Ngo> nearbyNgos = ngoRepository.findByCityContainingIgnoreCase(donation.getCity());
        for (Ngo ngo : nearbyNgos) {
            if (ngo.getUser() != null) {
                notificationService.sendNotification(
                        ngo.getUser().getId(),
                        "New Local Donation Available",
                        "A new " + donation.getCategory().toLowerCase() + " donation '" + donation.getTitle() + "' is available in " + donation.getCity() + ".",
                        "DONATION_POSTED",
                        "DONATION",
                        donation.getId()
                );
            }
        }

        return mapToResponse(donation, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationResponse> getMarketplaceDonations(String city, String category) {
        List<Donation> donations;
        if (city != null && !city.isBlank()) {
            donations = donationRepository.findByCityContainingIgnoreCaseAndStatus(city, "AVAILABLE");
        } else if (category != null && !category.isBlank()) {
            donations = donationRepository.findByCategoryAndStatus(category.toUpperCase(), "AVAILABLE");
        } else {
            donations = donationRepository.findByStatus("AVAILABLE");
        }

        // Marketplace views DO NOT expose private pickup codes!
        return donations.stream().map(d -> mapToResponse(d, false)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DonationResponse getDonationById(Long donationId, Long currentUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        boolean canSeePickupCode = isAuthorizedToSeePickupCode(donation, currentUserId);
        return mapToResponse(donation, canSeePickupCode);
    }

    @Override
    @Transactional
    public ClaimResponse claimDonation(Long donationId, Long ngoUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        Ngo ngo = ngoRepository.findByUserId(ngoUserId)
                .orElseThrow(() -> new ForbiddenException("Only registered NGOs can claim donations."));

        if ("REJECTED".equalsIgnoreCase(ngo.getVerificationStatus())) {
            throw new ForbiddenException("Rejected NGO profiles cannot claim donations from the marketplace.");
        }

        if (!"AVAILABLE".equals(donation.getStatus()) || donationClaimRepository.existsByDonationId(donationId)) {
            throw new ConflictException("Donation has already been claimed by another NGO.");
        }

        donation.setStatus("CLAIMED");
        donation.setClaimedAt(LocalDateTime.now());
        donationRepository.save(donation);

        // Update corresponding FoodAvailability status to CLAIMED if present
        restaurantRepository.findByUserId(donation.getDonorUser().getId()).ifPresent(rest -> {
            List<FoodAvailability> foodList = foodAvailabilityRepository.findByRestaurantId(rest.getId());
            for (FoodAvailability fa : foodList) {
                if ("AVAILABLE".equals(fa.getStatus()) && fa.getFoodType().equalsIgnoreCase(donation.getTitle())) {
                    fa.setStatus("CLAIMED");
                    foodAvailabilityRepository.save(fa);
                    break;
                }
            }
        });

        DonationClaim claim = DonationClaim.builder()
                .donation(donation)
                .ngo(ngo)
                .status("CLAIMED")
                .build();
        claim = donationClaimRepository.save(claim);

        // Record status history
        DonationStatusHistory history = DonationStatusHistory.builder()
                .donation(donation)
                .status("CLAIMED")
                .changedByUser(ngo.getUser())
                .remarks("Claimed by NGO: " + ngo.getNgoName())
                .build();
        donationStatusHistoryRepository.save(history);

        // Send notifications
        notificationService.sendNotification(
                donation.getDonorUser().getId(),
                "Donation Claimed!",
                ngo.getNgoName() + " has claimed your donation '" + donation.getTitle() + "'. They may assign a volunteer for pickup soon.",
                "DONATION_CLAIMED",
                "DONATION",
                donation.getId()
        );

        notificationService.sendNotification(
                ngo.getUser().getId(),
                "Claim Successful",
                "You have successfully claimed donation '" + donation.getTitle() + "'. Pickup Code: " + donation.getPickupCode(),
                "DONATION_CLAIMED",
                "DONATION",
                donation.getId()
        );

        return ClaimResponse.builder()
                .id(claim.getId())
                .donationId(donation.getId())
                .donationTitle(donation.getTitle())
                .ngoId(ngo.getId())
                .ngoName(ngo.getNgoName())
                .pickupCode(donation.getPickupCode())
                .status("CLAIMED")
                .claimedAt(claim.getClaimedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationResponse> getDonationsByDonor(Long donorUserId) {
        return donationRepository.findByDonorUserId(donorUserId)
                .stream().map(d -> mapToResponse(d, true)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonationResponse> getClaimedDonationsByNgo(Long ngoUserId) {
        Ngo ngo = ngoRepository.findByUserId(ngoUserId)
                .orElseThrow(() -> new ForbiddenException("Only registered NGOs can view claimed donations."));

        List<DonationClaim> claims = donationClaimRepository.findByNgoId(ngo.getId());
        return claims.stream()
                .map(claim -> mapToResponse(claim.getDonation(), true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DonationResponse confirmDonorHandover(Long donationId, String pickupCode, Long currentUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isDonor = donation.getDonorUser().getId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole().getName());

        if (!isDonor && !isAdmin) {
            throw new ForbiddenException("Only the original donor can confirm physical handover of this donation.");
        }

        if ("AVAILABLE".equalsIgnoreCase(donation.getStatus())) {
            throw new BadRequestException("Donation has not been claimed yet.");
        }

        if ("PICKUP".equalsIgnoreCase(donation.getStatus())) {
            throw new BadRequestException("Handover has already been confirmed.");
        }

        if ("COMPLETED".equalsIgnoreCase(donation.getStatus())) {
            throw new BadRequestException("Donation pickup and delivery have already been completed.");
        }

        if (pickupCode == null || !donation.getPickupCode().trim().equalsIgnoreCase(pickupCode.trim())) {
            throw new BadRequestException("Invalid pickup verification code.");
        }

        VolunteerTask task = volunteerTaskRepository.findFirstByDonationIdOrderByIdDesc(donationId).orElse(null);
        if (task == null) {
            throw new BadRequestException("No volunteer has been assigned to collect this donation yet.");
        }

        donation.setStatus("PICKUP");
        donation.setPickedUpAt(LocalDateTime.now());
        donationRepository.save(donation);

        if ("ASSIGNED".equalsIgnoreCase(task.getStatus()) || "ACCEPTED".equalsIgnoreCase(task.getStatus())) {
            task.setStatus("IN_PROGRESS");
            volunteerTaskRepository.save(task);
        }

        String volunteerName = task.getVolunteer() != null && task.getVolunteer().getUser() != null
                ? task.getVolunteer().getUser().getName() : "Volunteer";

        DonationStatusHistory history = DonationStatusHistory.builder()
                .donation(donation)
                .status("PICKUP")
                .changedByUser(user)
                .remarks("Physical handover confirmed by donor to volunteer: " + volunteerName)
                .build();
        donationStatusHistoryRepository.save(history);

        // Notify Claiming NGO
        donationClaimRepository.findByDonationId(donationId).ifPresent(claim -> {
            if (claim.getNgo().getUser() != null) {
                notificationService.sendNotification(
                        claim.getNgo().getUser().getId(),
                        "Food Collected & In Transit",
                        volunteerName + " has collected donation '" + donation.getTitle() + "' and is on the way to your facility.",
                        "DONATION_PICKED_UP",
                        "DONATION",
                        donation.getId()
                );
            }
        });

        // Notify Volunteer
        if (task.getVolunteer() != null && task.getVolunteer().getUser() != null) {
            notificationService.sendNotification(
                    task.getVolunteer().getUser().getId(),
                    "Handover Confirmed",
                    "Donor has confirmed physical handover of '" + donation.getTitle() + "'. Please deliver to claiming NGO.",
                    "DONATION_PICKED_UP",
                    "DONATION",
                    donation.getId()
            );
        }

        return mapToResponse(donation, true);
    }

    @Override
    @Transactional
    public DonationResponse confirmNgoReceipt(Long donationId, String pickupCode, Long currentUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        DonationClaim claim = donationClaimRepository.findByDonationId(donationId)
                .orElseThrow(() -> new BadRequestException("Donation has not been claimed."));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isClaimingNgo = claim.getNgo().getUser().getId().equals(currentUserId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole().getName());

        if (!isClaimingNgo && !isAdmin) {
            throw new ForbiddenException("Only the claiming NGO can confirm delivery receipt for this donation.");
        }

        if ("COMPLETED".equalsIgnoreCase(donation.getStatus())) {
            throw new BadRequestException("Donation pickup and delivery have already been completed.");
        }

        if (!"PICKUP".equalsIgnoreCase(donation.getStatus())) {
            throw new BadRequestException("Cannot confirm receipt: food handover by donor to volunteer has not been confirmed yet.");
        }

        if (pickupCode == null || !donation.getPickupCode().trim().equalsIgnoreCase(pickupCode.trim())) {
            throw new BadRequestException("Invalid pickup verification code.");
        }

        donation.setStatus("COMPLETED");
        donation.setCompletedAt(LocalDateTime.now());
        donationRepository.save(donation);

        VolunteerTask task = volunteerTaskRepository.findFirstByDonationIdOrderByIdDesc(donationId).orElse(null);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
            if (Boolean.TRUE.equals(task.getIsPaid())) {
                task.setPaymentStatus("PENDING");
            }
            volunteerTaskRepository.save(task);
        }

        DonationStatusHistory history = DonationStatusHistory.builder()
                .donation(donation)
                .status("COMPLETED")
                .changedByUser(user)
                .remarks("Delivery receipt confirmed by NGO: " + claim.getNgo().getNgoName())
                .build();
        donationStatusHistoryRepository.save(history);

        // Notify Donor
        notificationService.sendNotification(
                donation.getDonorUser().getId(),
                "Donation Delivered Successfully",
                "Your donation '" + donation.getTitle() + "' has reached " + claim.getNgo().getNgoName() + " safely.",
                "DONATION_DELIVERED",
                "DONATION",
                donation.getId()
        );

        // Notify Volunteer
        if (task != null && task.getVolunteer() != null && task.getVolunteer().getUser() != null) {
            notificationService.sendNotification(
                    task.getVolunteer().getUser().getId(),
                    "Delivery Receipt Confirmed",
                    claim.getNgo().getNgoName() + " has confirmed delivery receipt for donation '" + donation.getTitle() + "'.",
                    "DONATION_DELIVERED",
                    "DONATION",
                    donation.getId()
            );
        }

        return mapToResponse(donation, true);
    }

    @Override
    @Transactional
    public DonationResponse verifyPickupCode(Long donationId, String pickupCode, Long currentUserId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isDonor = donation.getDonorUser().getId().equals(currentUserId);
        boolean isClaimingNgo = donationClaimRepository.findByDonationId(donationId)
                .map(c -> c.getNgo().getUser().getId().equals(currentUserId))
                .orElse(false);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole().getName());

        if (isDonor) {
            return confirmDonorHandover(donationId, pickupCode, currentUserId);
        } else if (isClaimingNgo) {
            VolunteerTask task = volunteerTaskRepository.findFirstByDonationIdOrderByIdDesc(donationId).orElse(null);
            if (task == null && "CLAIMED".equalsIgnoreCase(donation.getStatus())) {
                // Direct NGO self-pickup legacy support (no volunteer assigned)
                if (pickupCode == null || !donation.getPickupCode().trim().equalsIgnoreCase(pickupCode.trim())) {
                    throw new BadRequestException("Invalid pickup verification code.");
                }
                donation.setStatus("COMPLETED");
                donation.setCompletedAt(LocalDateTime.now());
                donationRepository.save(donation);

                DonationStatusHistory history = DonationStatusHistory.builder()
                        .donation(donation)
                        .status("COMPLETED")
                        .changedByUser(user)
                        .remarks("Direct pickup completed by NGO")
                        .build();
                donationStatusHistoryRepository.save(history);

                notificationService.sendNotification(
                        donation.getDonorUser().getId(),
                        "Handover Confirmed!",
                        "Pickup code verified! Handover for donation '" + donation.getTitle() + "' is marked COMPLETED.",
                        "DONATION_STATUS_UPDATED",
                        "DONATION",
                        donation.getId()
                );
                return mapToResponse(donation, true);
            }
            return confirmNgoReceipt(donationId, pickupCode, currentUserId);
        } else if (isAdmin) {
            if ("CLAIMED".equalsIgnoreCase(donation.getStatus())) {
                return confirmDonorHandover(donationId, pickupCode, currentUserId);
            } else {
                return confirmNgoReceipt(donationId, pickupCode, currentUserId);
            }
        } else {
            throw new ForbiddenException("You are not authorized to verify this donation.");
        }
    }

    private boolean isAuthorizedToSeePickupCode(Donation donation, Long currentUserId) {
        if (currentUserId == null) return false;
        if (donation.getDonorUser().getId().equals(currentUserId)) return true;

        boolean isClaimingNgo = donationClaimRepository.findByDonationId(donation.getId())
                .map(claim -> claim.getNgo().getUser().getId().equals(currentUserId))
                .orElse(false);
        if (isClaimingNgo) return true;

        VolunteerTask task = volunteerTaskRepository.findFirstByDonationIdOrderByIdDesc(donation.getId()).orElse(null);
        if (task != null && task.getVolunteer() != null && task.getVolunteer().getUser() != null) {
            if (task.getVolunteer().getUser().getId().equals(currentUserId)) return true;
        }

        return userRepository.findById(currentUserId)
                .map(u -> "ADMIN".equalsIgnoreCase(u.getRole().getName()))
                .orElse(false);
    }

    private DonationResponse mapToResponse(Donation d, boolean showPickupCode) {
        String claimedNgoName = null;
        Long claimedNgoId = null;
        DonationClaim claim = donationClaimRepository.findByDonationId(d.getId()).orElse(null);
        if (claim != null) {
            claimedNgoId = claim.getNgo().getId();
            claimedNgoName = claim.getNgo().getNgoName();
        }

        Long assignedVolunteerId = null;
        String assignedVolunteerName = null;
        String assignedVolunteerContact = null;
        String taskStatus = null;
        VolunteerTask task = volunteerTaskRepository.findFirstByDonationIdOrderByIdDesc(d.getId()).orElse(null);
        if (task != null && task.getVolunteer() != null) {
            assignedVolunteerId = task.getVolunteer().getId();
            if (task.getVolunteer().getUser() != null) {
                assignedVolunteerName = task.getVolunteer().getUser().getName();
                assignedVolunteerContact = task.getVolunteer().getUser().getPhone();
            }
            taskStatus = task.getStatus();
        }

        return DonationResponse.builder()
                .id(d.getId())
                .donorUserId(d.getDonorUser().getId())
                .donorName(d.getDonorUser().getName())
                .donorRole(d.getDonorUser().getRole().getName())
                .donorContact(d.getDonorUser().getPhone())
                .restaurantBranchId(d.getRestaurantBranch() != null ? d.getRestaurantBranch().getId() : null)
                .branchName(d.getRestaurantBranch() != null ? d.getRestaurantBranch().getBranchName() : null)
                .category(d.getCategory())
                .title(d.getTitle())
                .description(d.getDescription())
                .quantity(d.getQuantity())
                .unit(d.getUnit())
                .address(d.getAddress())
                .city(d.getCity())
                .state(d.getState())
                .availabilityStart(d.getAvailabilityStart())
                .availabilityEnd(d.getAvailabilityEnd())
                .pickupCode(showPickupCode ? d.getPickupCode() : null)
                .status(d.getStatus())
                .claimedByNgoId(claimedNgoId)
                .claimedByNgoName(claimedNgoName)
                .claimedAt(d.getClaimedAt())
                .pickedUpAt(d.getPickedUpAt())
                .completedAt(d.getCompletedAt())
                .assignedVolunteerId(assignedVolunteerId)
                .assignedVolunteerName(assignedVolunteerName)
                .assignedVolunteerContact(assignedVolunteerContact)
                .taskStatus(taskStatus)
                .createdAt(d.getCreatedAt())
                .build();
    }
}
