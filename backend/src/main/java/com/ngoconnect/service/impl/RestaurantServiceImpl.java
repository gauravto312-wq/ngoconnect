package com.ngoconnect.service.impl;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.*;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.*;
import com.ngoconnect.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.ngoconnect.service.NotificationService;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantBranchRepository branchRepository;
    private final FoodAvailabilityRepository foodAvailabilityRepository;
    private final LocationRepository locationRepository;
    private final DonationRepository donationRepository;
    private final DonationStatusHistoryRepository donationStatusHistoryRepository;
    private final NgoRepository ngoRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public RestaurantProfileResponse getRestaurantProfile(Long userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found for user id: " + userId));
        return mapToResponse(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantProfileResponse> getPublicRestaurants(String search, String city) {
        List<Restaurant> restaurants;
        if ((search != null && !search.isBlank()) || (city != null && !city.isBlank())) {
            restaurants = restaurantRepository.searchRestaurants(search, city);
        } else {
            restaurants = restaurantRepository.findAll();
        }
        return restaurants.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantProfileResponse getPublicRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return mapToResponse(restaurant);
    }

    @Override
    @Transactional
    public RestaurantProfileResponse updatePhotos(Long userId, String profileImageUrl, String galleryPhotos) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found for user id: " + userId));

        if (profileImageUrl != null) {
            restaurant.setProfileImageUrl(profileImageUrl);
        }
        if (galleryPhotos != null) {
            restaurant.setGalleryPhotos(galleryPhotos);
        }

        restaurant = restaurantRepository.save(restaurant);
        return mapToResponse(restaurant);
    }

    @Override
    @Transactional
    public RestaurantBranchResponse addBranch(Long userId, RestaurantBranchRequest request) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found for user id: " + userId));

        Location location = Location.builder()
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .build();
        location = locationRepository.save(location);

        RestaurantBranch branch = RestaurantBranch.builder()
                .restaurant(restaurant)
                .branchName(request.getBranchName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .contactNumber(request.getContactNumber())
                .location(location)
                .isActive(true)
                .build();

        branch = branchRepository.save(branch);
        return mapToBranchResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantBranchResponse> getBranches(Long userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found for user id: " + userId));

        return branchRepository.findByRestaurantId(restaurant.getId())
                .stream().map(this::mapToBranchResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodAvailabilityResponse postFoodAvailability(Long userId, FoodAvailabilityRequest request) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found for user id: " + userId));

        if (!"APPROVED".equalsIgnoreCase(restaurant.getVerificationStatus())) {
            throw new ForbiddenException("Restaurant profile is pending admin verification. Only verified restaurants can post food availability.");
        }

        RestaurantBranch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.getBranchId()));
            if (!branch.getRestaurant().getId().equals(restaurant.getId())) {
                throw new ForbiddenException("Branch does not belong to your restaurant.");
            }
        }

        FoodAvailability availability = FoodAvailability.builder()
                .restaurant(restaurant)
                .branch(branch)
                .foodType(request.getFoodType())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null ? request.getUnit() : "meals")
                .availableFrom(request.getAvailableFrom())
                .availableUntil(request.getAvailableUntil())
                .pickupDeadline(request.getPickupDeadline())
                .status("AVAILABLE")
                .build();

        availability = foodAvailabilityRepository.save(availability);

        // Also publish to Marketplace as a Donation entity
        String pickupCode = "NGC-" + (100000 + new Random().nextInt(900000));
        String address = branch != null ? branch.getAddress() : restaurant.getAddress();
        String city = branch != null ? branch.getCity() : restaurant.getCity();
        String state = branch != null ? branch.getState() : restaurant.getState();

        Donation donation = Donation.builder()
                .donorUser(restaurant.getUser())
                .restaurantBranch(branch)
                .category("FOOD")
                .title(request.getFoodType())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null ? request.getUnit() : "meals")
                .address(address)
                .city(city)
                .state(state)
                .availabilityStart(request.getAvailableFrom())
                .availabilityEnd(request.getAvailableUntil())
                .pickupCode(pickupCode)
                .status("AVAILABLE")
                .build();

        donation = donationRepository.save(donation);

        DonationStatusHistory history = DonationStatusHistory.builder()
                .donation(donation)
                .status("AVAILABLE")
                .changedByUser(restaurant.getUser())
                .remarks("Food Availability published to Marketplace")
                .build();
        donationStatusHistoryRepository.save(history);

        // Notify nearby NGOs registered in the city
        List<Ngo> nearbyNgos = ngoRepository.findByCityContainingIgnoreCase(city);
        for (Ngo ngo : nearbyNgos) {
            if (ngo.getUser() != null) {
                notificationService.sendNotification(
                        ngo.getUser().getId(),
                        "New Restaurant Food Available",
                        restaurant.getRestaurantName() + " posted available food '" + request.getFoodType() + "' in " + city + ".",
                        "DONATION_POSTED",
                        "DONATION",
                        donation.getId()
                );
            }
        }

        return mapToAvailabilityResponse(availability);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodAvailabilityResponse> getFoodAvailabilities(Long userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found for user id: " + userId));

        return foodAvailabilityRepository.findByRestaurantId(restaurant.getId())
                .stream().map(this::mapToAvailabilityResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendPublicInquiry(Long restaurantId, String senderName, String senderContact, String messageText) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        if (restaurant.getUser() != null) {
            String fullMessage = "From: " + senderName + " (" + senderContact + ")\nMessage: " + messageText;
            notificationService.sendNotification(
                    restaurant.getUser().getId(),
                    "New Public Message / Inquiry from " + senderName,
                    fullMessage,
                    "DIRECT_INQUIRY",
                    "RESTAURANT",
                    restaurant.getId()
            );
        }
    }

    private RestaurantProfileResponse mapToResponse(Restaurant r) {
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

    private RestaurantBranchResponse mapToBranchResponse(RestaurantBranch b) {
        return RestaurantBranchResponse.builder()
                .id(b.getId())
                .restaurantId(b.getRestaurant().getId())
                .restaurantName(b.getRestaurant().getRestaurantName())
                .branchName(b.getBranchName())
                .address(b.getAddress())
                .city(b.getCity())
                .state(b.getState())
                .contactNumber(b.getContactNumber())
                .isActive(b.getIsActive())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private FoodAvailabilityResponse mapToAvailabilityResponse(FoodAvailability fa) {
        return FoodAvailabilityResponse.builder()
                .id(fa.getId())
                .restaurantId(fa.getRestaurant().getId())
                .restaurantName(fa.getRestaurant().getRestaurantName())
                .branchId(fa.getBranch() != null ? fa.getBranch().getId() : null)
                .branchName(fa.getBranch() != null ? fa.getBranch().getBranchName() : null)
                .foodType(fa.getFoodType())
                .description(fa.getDescription())
                .quantity(fa.getQuantity())
                .unit(fa.getUnit())
                .availableFrom(fa.getAvailableFrom())
                .availableUntil(fa.getAvailableUntil())
                .pickupDeadline(fa.getPickupDeadline())
                .status(fa.getStatus())
                .createdAt(fa.getCreatedAt())
                .build();
    }
}
