package com.ngoconnect.foodavailability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngoconnect.dto.AuthResponse;
import com.ngoconnect.dto.FoodAvailabilityRequest;
import com.ngoconnect.dto.RegisterRequest;
import com.ngoconnect.entity.*;
import com.ngoconnect.repository.*;
import com.ngoconnect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FoodAvailabilityMarketplaceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantBranchRepository branchRepository;

    @Autowired
    private FoodAvailabilityRepository foodAvailabilityRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private DonationClaimRepository donationClaimRepository;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private UserRepository userRepository;

    private User approvedRestaurantUser;
    private User pendingRestaurantUser;
    private Restaurant approvedRestaurant;
    private Restaurant pendingRestaurant;
    private Ngo approvedNgo;

    private AuthResponse approvedRestaurantAuth;
    private AuthResponse pendingRestaurantAuth;
    private AuthResponse approvedNgoAuth;
    private AuthResponse pendingNgoAuth;
    private AuthResponse citizenAuth;

    @BeforeEach
    void setUp() {
        donationClaimRepository.deleteAll();
        donationRepository.deleteAll();
        foodAvailabilityRepository.deleteAll();
        branchRepository.deleteAll();
        restaurantRepository.deleteAll();
        ngoRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register Approved Restaurant
        RegisterRequest restReq = new RegisterRequest();
        restReq.setName("Tasty Bites Owner");
        restReq.setEmail("tasty@restaurant.com");
        restReq.setPhone("9876543210");
        restReq.setPassword("Password123!");
        restReq.setRole("RESTAURANT");
        restReq.setRestaurantName("Tasty Bites Lucknow");
        restReq.setCity("Lucknow");
        restReq.setAddress("Hazratganj");
        approvedRestaurantAuth = authService.register(restReq);

        approvedRestaurantUser = userRepository.findByEmail("tasty@restaurant.com").orElseThrow();
        approvedRestaurant = restaurantRepository.findByUserId(approvedRestaurantUser.getId()).orElseThrow();
        approvedRestaurant.setVerificationStatus("APPROVED");
        approvedRestaurant = restaurantRepository.save(approvedRestaurant);

        // 2. Register Pending Restaurant
        RegisterRequest pendingRestReq = new RegisterRequest();
        pendingRestReq.setName("Unverified Diner Owner");
        pendingRestReq.setEmail("pending@restaurant.com");
        pendingRestReq.setPhone("9876543211");
        pendingRestReq.setPassword("Password123!");
        pendingRestReq.setRole("RESTAURANT");
        pendingRestReq.setRestaurantName("Unverified Diner");
        pendingRestReq.setCity("Lucknow");
        pendingRestReq.setAddress("Gomti Nagar");
        pendingRestaurantAuth = authService.register(pendingRestReq);

        pendingRestaurantUser = userRepository.findByEmail("pending@restaurant.com").orElseThrow();
        pendingRestaurant = restaurantRepository.findByUserId(pendingRestaurantUser.getId()).orElseThrow();

        // 3. Register Approved NGO
        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("Hope Welfare Leader");
        ngoReq.setEmail("hope@ngo.org");
        ngoReq.setPhone("9998887776");
        ngoReq.setPassword("Password123!");
        ngoReq.setRole("NGO");
        ngoReq.setNgoName("Hope Welfare Foundation");
        ngoReq.setCity("Lucknow");
        ngoReq.setAddress("Aliganj");
        approvedNgoAuth = authService.register(ngoReq);

        User approvedNgoUser = userRepository.findByEmail("hope@ngo.org").orElseThrow();
        approvedNgo = ngoRepository.findByUserId(approvedNgoUser.getId()).orElseThrow();
        approvedNgo.setVerificationStatus("APPROVED");
        approvedNgo = ngoRepository.save(approvedNgo);

        // 4. Register Pending NGO
        RegisterRequest pendingNgoReq = new RegisterRequest();
        pendingNgoReq.setName("Pending NGO Leader");
        pendingNgoReq.setEmail("pending@ngo.org");
        pendingNgoReq.setPhone("9998887775");
        pendingNgoReq.setPassword("Password123!");
        pendingNgoReq.setRole("NGO");
        pendingNgoReq.setNgoName("Pending Relief Society");
        pendingNgoReq.setCity("Lucknow");
        pendingNgoReq.setAddress("Indira Nagar");
        pendingNgoAuth = authService.register(pendingNgoReq);

        // 5. Register Citizen
        RegisterRequest citizenReq = new RegisterRequest();
        citizenReq.setName("General Citizen");
        citizenReq.setEmail("citizen.food@example.com");
        citizenReq.setPhone("9876543214");
        citizenReq.setPassword("Password123!");
        citizenReq.setRole("CITIZEN");
        citizenAuth = authService.register(citizenReq);
    }

    @Test
    void testVerifiedRestaurant_PostsFoodAvailability_AppearsInMarketplace() throws Exception {
        FoodAvailabilityRequest request = new FoodAvailabilityRequest();
        request.setFoodType("Fresh Veg Thali Packets");
        request.setDescription("50 surplus packed lunch meals ready for immediate pickup.");
        request.setQuantity(50);
        request.setUnit("meals");
        request.setAvailableFrom(LocalDateTime.now());
        request.setAvailableUntil(LocalDateTime.now().plusHours(6));
        request.setPickupDeadline(LocalDateTime.now().plusHours(4));

        mockMvc.perform(post("/api/v1/restaurants/food-availability")
                        .header("Authorization", "Bearer " + approvedRestaurantAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        List<FoodAvailability> foodList = foodAvailabilityRepository.findByRestaurantId(approvedRestaurant.getId());
        assertEquals(1, foodList.size());
        assertEquals("AVAILABLE", foodList.get(0).getStatus());

        List<Donation> marketplaceList = donationRepository.findByStatus("AVAILABLE");
        assertEquals(1, marketplaceList.size());
        Donation marketplaceItem = marketplaceList.get(0);
        assertEquals("Fresh Veg Thali Packets", marketplaceItem.getTitle());
        assertEquals("Lucknow", marketplaceItem.getCity());
        assertEquals(50, marketplaceItem.getQuantity());

        mockMvc.perform(get("/api/v1/donations/marketplace"))
                .andExpect(status().isOk());
    }

    @Test
    void testUnverifiedRestaurant_PostFoodAvailability_ReturnsForbidden403() throws Exception {
        FoodAvailabilityRequest request = new FoodAvailabilityRequest();
        request.setFoodType("Unverified Meals");
        request.setQuantity(10);
        request.setAvailableFrom(LocalDateTime.now());
        request.setAvailableUntil(LocalDateTime.now().plusHours(6));
        request.setPickupDeadline(LocalDateTime.now().plusHours(4));

        mockMvc.perform(post("/api/v1/restaurants/food-availability")
                        .header("Authorization", "Bearer " + pendingRestaurantAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidQuantity_PostFoodAvailability_ReturnsBadRequest400() throws Exception {
        FoodAvailabilityRequest request = new FoodAvailabilityRequest();
        request.setFoodType("Negative Meals");
        request.setQuantity(-5);
        request.setAvailableFrom(LocalDateTime.now());
        request.setAvailableUntil(LocalDateTime.now().plusHours(6));
        request.setPickupDeadline(LocalDateTime.now().plusHours(4));

        mockMvc.perform(post("/api/v1/restaurants/food-availability")
                        .header("Authorization", "Bearer " + approvedRestaurantAuth.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEligibleNgo_ClaimsFoodAvailability_SuccessAndUpdatesStateToClaimed() throws Exception {
        Donation donation = donationRepository.save(Donation.builder()
                .donorUser(approvedRestaurantUser)
                .category("FOOD")
                .title("Dal Chawal Boxes")
                .description("30 boxes")
                .quantity(30)
                .unit("boxes")
                .address("Hazratganj Main Market")
                .city("Lucknow")
                .state("Uttar Pradesh")
                .pickupCode("NGC-123456")
                .status("AVAILABLE")
                .build());

        FoodAvailability fa = foodAvailabilityRepository.save(FoodAvailability.builder()
                .restaurant(approvedRestaurant)
                .foodType("Dal Chawal Boxes")
                .quantity(30)
                .availableFrom(LocalDateTime.now())
                .availableUntil(LocalDateTime.now().plusHours(6))
                .pickupDeadline(LocalDateTime.now().plusHours(4))
                .status("AVAILABLE")
                .build());

        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/claim")
                        .header("Authorization", "Bearer " + approvedNgoAuth.getToken()))
                .andExpect(status().isOk());

        Donation updatedDonation = donationRepository.findById(donation.getId()).orElseThrow();
        assertEquals("CLAIMED", updatedDonation.getStatus());

        FoodAvailability updatedFa = foodAvailabilityRepository.findById(fa.getId()).orElseThrow();
        assertEquals("CLAIMED", updatedFa.getStatus());
    }

    @Test
    void testNonNgo_ClaimsFoodAvailability_ReturnsForbidden403() throws Exception {
        Donation donation = donationRepository.save(Donation.builder()
                .donorUser(approvedRestaurantUser)
                .category("FOOD")
                .title("Burger Combos")
                .quantity(15)
                .unit("meals")
                .address("Hazratganj Main Market")
                .city("Lucknow")
                .state("Uttar Pradesh")
                .pickupCode("NGC-654321")
                .status("AVAILABLE")
                .build());

        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/claim")
                        .header("Authorization", "Bearer " + citizenAuth.getToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDuplicateClaim_ReturnsConflict409() throws Exception {
        Donation donation = donationRepository.save(Donation.builder()
                .donorUser(approvedRestaurantUser)
                .category("FOOD")
                .title("Pizza Boxes")
                .quantity(20)
                .unit("boxes")
                .address("Hazratganj Main Market")
                .city("Lucknow")
                .state("Uttar Pradesh")
                .pickupCode("NGC-987654")
                .status("AVAILABLE")
                .build());

        // NGO 1 claims successfully
        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/claim")
                        .header("Authorization", "Bearer " + approvedNgoAuth.getToken()))
                .andExpect(status().isOk());

        // NGO 2 attempts duplicate claim
        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/claim")
                        .header("Authorization", "Bearer " + approvedNgoAuth.getToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void testRejectedNgo_ClaimsFoodAvailability_ReturnsForbidden403() throws Exception {
        User pendingNgoUser = userRepository.findByEmail("pending@ngo.org").orElseThrow();
        Ngo ngo = ngoRepository.findByUserId(pendingNgoUser.getId()).orElseThrow();
        ngo.setVerificationStatus("REJECTED");
        ngoRepository.save(ngo);

        Donation donation = donationRepository.save(Donation.builder()
                .donorUser(approvedRestaurantUser)
                .category("FOOD")
                .title("Roti Sabzi Meals")
                .quantity(40)
                .unit("meals")
                .address("Hazratganj Main Market")
                .city("Lucknow")
                .state("Uttar Pradesh")
                .pickupCode("NGC-112233")
                .status("AVAILABLE")
                .build());

        mockMvc.perform(post("/api/v1/donations/" + donation.getId() + "/claim")
                        .header("Authorization", "Bearer " + pendingNgoAuth.getToken()))
                .andExpect(status().isForbidden());
    }
}
