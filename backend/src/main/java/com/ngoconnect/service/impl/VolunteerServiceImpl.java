package com.ngoconnect.service.impl;

import com.ngoconnect.dto.VolunteerEarningsResponse;
import com.ngoconnect.dto.VolunteerProfileResponse;
import com.ngoconnect.dto.VolunteerStatusUpdateRequest;
import com.ngoconnect.entity.Volunteer;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.VolunteerRepository;
import com.ngoconnect.service.TaskService;
import com.ngoconnect.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements VolunteerService {

    private final VolunteerRepository volunteerRepository;
    @Lazy
    private final TaskService taskService;

    @Override
    @Transactional(readOnly = true)
    public VolunteerEarningsResponse getVolunteerEarnings(Long userId) {
        return taskService.getVolunteerEarnings(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public VolunteerProfileResponse getVolunteerProfile(Long userId) {
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user id: " + userId));
        return mapToResponse(volunteer);
    }

    @Override
    @Transactional
    public VolunteerProfileResponse updateStatus(Long userId, VolunteerStatusUpdateRequest request) {
        Volunteer volunteer = volunteerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user id: " + userId));

        volunteer.setAvailabilityStatus(request.getAvailabilityStatus().toUpperCase());
        volunteer = volunteerRepository.save(volunteer);
        return mapToResponse(volunteer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerProfileResponse> searchActiveVolunteers(String city) {
        List<Volunteer> volunteers;
        if (city != null && !city.isBlank()) {
            volunteers = volunteerRepository.findByCityContainingIgnoreCaseAndAvailabilityStatus(city, "ACTIVE");
        } else {
            volunteers = volunteerRepository.findByAvailabilityStatus("ACTIVE");
        }
        return volunteers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private VolunteerProfileResponse mapToResponse(Volunteer v) {
        return VolunteerProfileResponse.builder()
                .id(v.getId())
                .userId(v.getUser().getId())
                .name(v.getUser().getName())
                .email(v.getUser().getEmail())
                .phone(v.getUser().getPhone())
                .volunteerCode(v.getVolunteerCode())
                .city(v.getCity())
                .state(v.getState())
                .address(v.getAddress())
                .availabilityStatus(v.getAvailabilityStatus())
                .build();
    }
}
