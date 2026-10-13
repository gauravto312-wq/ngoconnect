package com.ngoconnect.service;

import com.ngoconnect.dto.*;
import java.util.List;

public interface RestaurantService {
    RestaurantProfileResponse getRestaurantProfile(Long userId);
    List<RestaurantProfileResponse> getPublicRestaurants(String search, String city);
    RestaurantProfileResponse getPublicRestaurantById(Long id);
    RestaurantProfileResponse updatePhotos(Long userId, String profileImageUrl, String galleryPhotos);
    RestaurantBranchResponse addBranch(Long userId, RestaurantBranchRequest request);
    List<RestaurantBranchResponse> getBranches(Long userId);
    FoodAvailabilityResponse postFoodAvailability(Long userId, FoodAvailabilityRequest request);
    List<FoodAvailabilityResponse> getFoodAvailabilities(Long userId);
    void sendPublicInquiry(Long restaurantId, String senderName, String senderContact, String messageText);
}
