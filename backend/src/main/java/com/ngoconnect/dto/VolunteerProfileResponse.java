package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VolunteerProfileResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String volunteerCode; // e.g. VOL-10025
    private String city;
    private String state;
    private String address;
    private String availabilityStatus; // ACTIVE, INACTIVE
}
