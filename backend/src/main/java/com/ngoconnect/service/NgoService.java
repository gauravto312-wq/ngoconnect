package com.ngoconnect.service;

import com.ngoconnect.dto.NgoProfileResponse;
import com.ngoconnect.dto.NgoVerificationRequest;
import com.ngoconnect.dto.NgoVerificationResponse;
import java.util.List;

public interface NgoService {
    NgoProfileResponse getNgoProfile(Long userId);
    List<NgoProfileResponse> getPublicNgos(String search, String city);
    NgoProfileResponse getPublicNgoById(Long id);
    NgoProfileResponse updatePhotos(Long userId, String profileImageUrl, String galleryPhotos);
    NgoVerificationResponse submitVerificationDocument(Long userId, NgoVerificationRequest request);
    List<NgoVerificationResponse> getNgoVerificationDocuments(Long userId);
    void sendPublicInquiry(Long ngoId, String senderName, String senderContact, String messageText);
}
