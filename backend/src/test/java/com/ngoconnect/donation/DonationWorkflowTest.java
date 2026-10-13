package com.ngoconnect.donation;

import com.ngoconnect.dto.*;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.repository.NgoRepository;
import com.ngoconnect.repository.UserRepository;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.DonationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class DonationWorkflowTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DonationService donationService;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private UserRepository userRepository;

    private AuthResponse donorAuth;
    private AuthResponse ngo1Auth;
    private AuthResponse ngo2Auth;
    private AuthResponse citizenAuth;

    @BeforeEach
    void setUp() {
        RegisterRequest donorReq = new RegisterRequest();
        donorReq.setName("Bake House Restaurant");
        donorReq.setEmail("donor.bakehouse@ngoconnect.org");
        donorReq.setPhone("9876543210");
        donorReq.setPassword("Password123!");
        donorReq.setRole("RESTAURANT");
        donorReq.setRestaurantName("Bake House");
        donorReq.setCity("Lucknow");
        donorReq.setAddress("Hazratganj");
        donorAuth = authService.register(donorReq);

        RegisterRequest ngo1Req = new RegisterRequest();
        ngo1Req.setName("Food For All NGO");
        ngo1Req.setEmail("ngo1.foodforall@ngoconnect.org");
        ngo1Req.setPhone("9876543211");
        ngo1Req.setPassword("Password123!");
        ngo1Req.setRole("NGO");
        ngo1Req.setNgoName("Food For All");
        ngo1Req.setCity("Lucknow");
        ngo1Req.setAddress("Indira Nagar");
        ngo1Auth = authService.register(ngo1Req);

        userRepository.findByEmail("ngo1.foodforall@ngoconnect.org").ifPresent(u -> {
            ngoRepository.findByUserId(u.getId()).ifPresent(ngo -> {
                ngo.setVerificationStatus("APPROVED");
                ngoRepository.save(ngo);
            });
        });

        RegisterRequest ngo2Req = new RegisterRequest();
        ngo2Req.setName("Helping Hands NGO");
        ngo2Req.setEmail("ngo2.helpinghands@ngoconnect.org");
        ngo2Req.setPhone("9876543212");
        ngo2Req.setPassword("Password123!");
        ngo2Req.setRole("NGO");
        ngo2Req.setNgoName("Helping Hands");
        ngo2Req.setCity("Lucknow");
        ngo2Req.setAddress("Gomti Nagar");
        ngo2Auth = authService.register(ngo2Req);

        userRepository.findByEmail("ngo2.helpinghands@ngoconnect.org").ifPresent(u -> {
            ngoRepository.findByUserId(u.getId()).ifPresent(ngo -> {
                ngo.setVerificationStatus("APPROVED");
                ngoRepository.save(ngo);
            });
        });

        RegisterRequest citizenReq = new RegisterRequest();
        citizenReq.setName("Citizen Sam");
        citizenReq.setEmail("citizen.sam@ngoconnect.org");
        citizenReq.setPhone("9876543213");
        citizenReq.setPassword("Password123!");
        citizenReq.setRole("CITIZEN");
        citizenAuth = authService.register(citizenReq);
    }

    @Test
    @DisplayName("shouldCreateDonationSuccessfully")
    void shouldCreateDonationSuccessfully() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Fresh Surplus Meals");
        req.setDescription("50 vegetarian meals ready for pickup");
        req.setQuantity(50);
        req.setUnit("meals");
        req.setAddress("Hazratganj Main Market");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");

        DonationResponse resp = donationService.createDonation(donorAuth.getId(), req);

        assertNotNull(resp);
        assertNotNull(resp.getId());
        assertEquals("AVAILABLE", resp.getStatus());
        assertEquals("Fresh Surplus Meals", resp.getTitle());
        assertNotNull(resp.getPickupCode()); // Donor can see pickup code
    }

    @Test
    @DisplayName("shouldHidePickupCodeInMarketplaceView")
    void shouldHidePickupCodeInMarketplaceView() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Bread Packets");
        req.setQuantity(20);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        donationService.createDonation(donorAuth.getId(), req);

        List<DonationResponse> marketplace = donationService.getMarketplaceDonations("Lucknow", null);

        assertFalse(marketplace.isEmpty());
        DonationResponse item = marketplace.get(0);
        assertNull(item.getPickupCode()); // Pickup code hidden from public marketplace!
    }

    @Test
    @DisplayName("shouldAllowEligibleNgoToClaimAvailableDonation")
    void shouldAllowEligibleNgoToClaimAvailableDonation() {
        DonationRequest req = new DonationRequest();
        req.setCategory("CLOTHES");
        req.setTitle("Winter Blankets");
        req.setQuantity(30);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(donorAuth.getId(), req);

        ClaimResponse claim = donationService.claimDonation(donation.getId(), ngo1Auth.getId());

        assertNotNull(claim);
        assertEquals("CLAIMED", claim.getStatus());
        assertEquals("Food For All", claim.getNgoName());
        assertNotNull(claim.getPickupCode()); // Claiming NGO receives pickup code
    }

    @Test
    @DisplayName("shouldRejectDuplicateClaimForSameDonation")
    void shouldRejectDuplicateClaimForSameDonation() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Rice Packets");
        req.setQuantity(15);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(donorAuth.getId(), req);

        donationService.claimDonation(donation.getId(), ngo1Auth.getId());

        // NGO 2 attempts to claim the same donation
        assertThrows(BadRequestException.class, () -> donationService.claimDonation(donation.getId(), ngo2Auth.getId()));
    }

    @Test
    @DisplayName("shouldVerifyPickupCodeAndCompleteDonation")
    void shouldVerifyPickupCodeAndCompleteDonation() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Cooked Meals");
        req.setQuantity(25);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(donorAuth.getId(), req);

        ClaimResponse claim = donationService.claimDonation(donation.getId(), ngo1Auth.getId());
        String code = claim.getPickupCode();

        DonationResponse completed = donationService.verifyPickupCode(donation.getId(), code, ngo1Auth.getId());

        assertNotNull(completed);
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    @DisplayName("shouldRejectIncorrectPickupCode")
    void shouldRejectIncorrectPickupCode() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Juice Bottles");
        req.setQuantity(10);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(donorAuth.getId(), req);

        donationService.claimDonation(donation.getId(), ngo1Auth.getId());

        assertThrows(BadRequestException.class, () -> donationService.verifyPickupCode(donation.getId(), "WRONG-CODE", ngo1Auth.getId()));
    }

    @Test
    @DisplayName("shouldRejectPickupCodeVerificationForAlreadyCompletedDonation")
    void shouldRejectPickupCodeVerificationForAlreadyCompletedDonation() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Canned Goods");
        req.setQuantity(40);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(donorAuth.getId(), req);

        ClaimResponse claim = donationService.claimDonation(donation.getId(), ngo1Auth.getId());
        String code = claim.getPickupCode();

        donationService.verifyPickupCode(donation.getId(), code, ngo1Auth.getId());

        // Attempting to complete again
        assertThrows(BadRequestException.class, () -> donationService.verifyPickupCode(donation.getId(), code, ngo1Auth.getId()));
    }

    @Test
    @DisplayName("shouldRejectNonNgoUserClaimingDonation")
    void shouldRejectNonNgoUserClaimingDonation() {
        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Snack Boxes");
        req.setQuantity(10);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        DonationResponse donation = donationService.createDonation(donorAuth.getId(), req);

        assertThrows(ForbiddenException.class, () -> donationService.claimDonation(donation.getId(), citizenAuth.getId()));
    }
}
