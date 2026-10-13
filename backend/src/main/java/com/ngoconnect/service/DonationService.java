package com.ngoconnect.service;

import com.ngoconnect.dto.ClaimResponse;
import com.ngoconnect.dto.DonationRequest;
import com.ngoconnect.dto.DonationResponse;
import java.util.List;

public interface DonationService {
    DonationResponse createDonation(Long userId, DonationRequest request);
    List<DonationResponse> getMarketplaceDonations(String city, String category);
    DonationResponse getDonationById(Long donationId, Long currentUserId);
    ClaimResponse claimDonation(Long donationId, Long ngoUserId);
    List<DonationResponse> getDonationsByDonor(Long donorUserId);
    List<DonationResponse> getClaimedDonationsByNgo(Long ngoUserId);
    DonationResponse confirmDonorHandover(Long donationId, String pickupCode, Long currentUserId);
    DonationResponse confirmNgoReceipt(Long donationId, String pickupCode, Long currentUserId);
    DonationResponse verifyPickupCode(Long donationId, String pickupCode, Long currentUserId);
}
