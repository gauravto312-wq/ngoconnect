package com.ngoconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FoodDriveResponse {
    private Long id;
    private Long createdByUserId;
    private String createdByName;
    private String createdByRole;
    private String title;
    private String description;
    private String purpose;
    private String address;
    private String city;
    private String state;
    private LocalDate eventDate;
    private String startTime;
    private String endTime;
    private String status; // UPCOMING, IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED
    private String coverImageUrl;
    private LocalDateTime completionDeadline;
    private LocalDateTime completedAt;
    private List<String> evidencePhotos;
    private Boolean isCreator;
    private Boolean photosSubmitted;
    private LocalDateTime photosSubmittedAt;
    private Integer participantCount;
    private Integer totalMealsContributed;
    private List<FoodDriveParticipantResponse> participants;
    private LocalDateTime createdAt;
}
