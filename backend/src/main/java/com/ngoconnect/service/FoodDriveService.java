package com.ngoconnect.service;

import com.ngoconnect.dto.FoodDriveParticipantRequest;
import com.ngoconnect.dto.FoodDriveParticipantResponse;
import com.ngoconnect.dto.FoodDrivePhotoRequest;
import com.ngoconnect.dto.FoodDriveRequest;
import com.ngoconnect.dto.FoodDriveResponse;
import java.util.List;

public interface FoodDriveService {
    FoodDriveResponse createFoodDrive(Long userId, FoodDriveRequest request);
    List<FoodDriveResponse> getFoodDrives(String city, String status, Long currentUserId);
    List<FoodDriveResponse> getFoodDrives(String city, String status);
    List<FoodDriveResponse> getFoodDrives(String city);
    FoodDriveResponse getFoodDriveById(Long id, Long currentUserId);
    FoodDriveResponse getFoodDriveById(Long id);
    FoodDriveParticipantResponse joinOrContribute(Long foodDriveId, Long userId, FoodDriveParticipantRequest request);
    List<FoodDriveResponse> getFoodDrivesByHost(Long userId);
    FoodDriveResponse addEvidencePhoto(Long foodDriveId, Long userId, FoodDrivePhotoRequest request);
    FoodDriveResponse removeEvidencePhoto(Long foodDriveId, Long userId, FoodDrivePhotoRequest request);
    FoodDriveResponse submitEvidencePhotos(Long foodDriveId, Long userId, FoodDrivePhotoRequest request);
    FoodDriveResponse completeFoodDrive(Long foodDriveId, Long userId);
}
