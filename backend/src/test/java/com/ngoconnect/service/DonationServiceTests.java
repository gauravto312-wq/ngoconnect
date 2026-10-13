package com.ngoconnect.service;

import com.ngoconnect.dto.*;
import com.ngoconnect.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

import com.ngoconnect.repository.NgoRepository;
import com.ngoconnect.repository.UserRepository;

@SpringBootTest(classes = com.ngoconnect.NGOConnectApplication.class)
@ActiveProfiles("test")
@Transactional
public class DonationServiceTests {

    @Autowired
    private DonationService donationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private NgoRepository ngoRepository;

    @Autowired
    private UserRepository userRepository;

    private AuthResponse registerUser(String name, String email, String role, String city) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPhone("9876543210");
        req.setPassword("Password123");
        req.setRole(role);
        req.setAddress("Test Address");
        req.setCity(city);
        req.setState("Uttar Pradesh");
        req.setNgoName(role.equals("NGO") ? name : null);
        req.setRestaurantName(role.equals("RESTAURANT") ? name : null);
        AuthResponse res = authService.register(req);

        if ("NGO".equals(role)) {
            userRepository.findByEmail(email).ifPresent(u -> {
                ngoRepository.findByUserId(u.getId()).ifPresent(ngo -> {
                    ngo.setVerificationStatus("APPROVED");
                    ngoRepository.save(ngo);
                });
            });
        }
        return res;
    }

    @Test
    @DisplayName("Phase 6: Create Donation & Verify Pickup Code Generation")
    void testCreateDonation() {
        AuthResponse donor = registerUser("Donor Citizen", "donor1@test.com", "CITIZEN", "Lucknow");

        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Fresh Food Packets");
        req.setQuantity(20);
        req.setUnit("packets");
        req.setAddress("Gomti Nagar");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");

        DonationResponse resp = donationService.createDonation(donor.getId(), req);

        assertNotNull(resp);
        assertEquals("AVAILABLE", resp.getStatus());
        assertNotNull(resp.getPickupCode());
        assertTrue(resp.getPickupCode().startsWith("NGC-"));
    }

    @Test
    @DisplayName("Phase 6 & 23: Public Marketplace Query - Pickup Code Hidden")
    void testMarketplaceHidesPickupCode() {
        AuthResponse donor = registerUser("Donor Citizen 2", "donor2@test.com", "CITIZEN", "Lucknow");

        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("Public Food Donation");
        req.setQuantity(10);
        req.setAddress("Hazratganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");

        donationService.createDonation(donor.getId(), req);

        List<DonationResponse> marketplace = donationService.getMarketplaceDonations("Lucknow", null);
        assertFalse(marketplace.isEmpty());

        for (DonationResponse d : marketplace) {
            assertNull(d.getPickupCode(), "Private pickup code MUST be hidden from public marketplace response!");
        }
    }

    @Test
    @DisplayName("Phase 6 & 7: NGO Claim & Concurrency Double Claim Prevention")
    void testNGOClaimAndConcurrency() {
        AuthResponse donor = registerUser("Donor Restaurant", "donor.rest@test.com", "RESTAURANT", "Lucknow");
        AuthResponse ngoA = registerUser("NGO Foundation A", "ngoa@test.com", "NGO", "Lucknow");
        AuthResponse ngoB = registerUser("NGO Foundation B", "ngob@test.com", "NGO", "Lucknow");

        DonationRequest req = new DonationRequest();
        req.setCategory("FOOD");
        req.setTitle("50 Thalis");
        req.setQuantity(50);
        req.setAddress("Aminabad");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");

        DonationResponse donResp = donationService.createDonation(donor.getId(), req);

        // NGO A Claims
        ClaimResponse claimResp = donationService.claimDonation(donResp.getId(), ngoA.getId());
        assertNotNull(claimResp);
        assertEquals("CLAIMED", claimResp.getStatus());
        assertNotNull(claimResp.getPickupCode());

        // NGO B attempts double claim on same donation -> MUST BE REJECTED
        assertThrows(BadRequestException.class, () -> donationService.claimDonation(donResp.getId(), ngoB.getId()));
    }

    @Test
    @DisplayName("Phase 6 & 23: Pickup Verification Code Handover")
    void testVerifyPickupCode() {
        AuthResponse donor = registerUser("Donor Citizen 3", "donor3@test.com", "CITIZEN", "Lucknow");
        AuthResponse ngo = registerUser("NGO Welfare", "ngo.welfare@test.com", "NGO", "Lucknow");

        DonationRequest req = new DonationRequest();
        req.setCategory("CLOTHES");
        req.setTitle("Winter Jackets");
        req.setQuantity(15);
        req.setAddress("Aliganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");

        DonationResponse donResp = donationService.createDonation(donor.getId(), req);
        ClaimResponse claim = donationService.claimDonation(donResp.getId(), ngo.getId());

        // Test invalid code -> Rejected
        assertThrows(BadRequestException.class, () -> 
            donationService.verifyPickupCode(donResp.getId(), "INVALID-CODE", ngo.getId())
        );

        // Test valid code -> Completed
        DonationResponse verified = donationService.verifyPickupCode(donResp.getId(), claim.getPickupCode(), ngo.getId());
        assertEquals("COMPLETED", verified.getStatus());
        assertNotNull(verified.getCompletedAt());
    }
}
