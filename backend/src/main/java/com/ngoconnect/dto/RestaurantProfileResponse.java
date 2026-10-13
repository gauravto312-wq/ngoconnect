package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantProfileResponse {
    private Long id;
    private Long userId;
    private String restaurantName;
    private String description;
    private String contactNumber;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String profileImageUrl;
    private String galleryPhotos;
    private String verificationStatus;
    private LocalDateTime createdAt;
}
