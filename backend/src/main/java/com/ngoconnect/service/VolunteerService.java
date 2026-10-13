package com.ngoconnect.service;

import com.ngoconnect.dto.VolunteerEarningsResponse;
import com.ngoconnect.dto.VolunteerProfileResponse;
import com.ngoconnect.dto.VolunteerStatusUpdateRequest;
import java.util.List;

public interface VolunteerService {
    VolunteerProfileResponse getVolunteerProfile(Long userId);
    VolunteerProfileResponse updateStatus(Long userId, VolunteerStatusUpdateRequest request);
    List<VolunteerProfileResponse> searchActiveVolunteers(String city);
    VolunteerEarningsResponse getVolunteerEarnings(Long userId);
}
