package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodDrivePhotoRequest {
    private String photoUrl;
    private List<String> photoUrls;

    public FoodDrivePhotoRequest(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
