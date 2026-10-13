package com.ngoconnect.service.impl;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.*;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.exception.ResourceNotFoundException;
import com.ngoconnect.repository.*;
import com.ngoconnect.service.FoodDriveService;
import com.ngoconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodDriveServiceImpl implements FoodDriveService {

    private final FoodDriveRepository foodDriveRepository;
    private final FoodDriveParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NgoRepository ngoRepository;
    private final RestaurantRepository restaurantRepository;
    private final VolunteerRepository volunteerRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FoodDriveResponse createFoodDrive(Long userId, FoodDriveRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String roleName = creator.getRole() != null ? creator.getRole().getName() : "";
        if (!"CITIZEN".equalsIgnoreCase(roleName) && !"NGO".equalsIgnoreCase(roleName) && !"ADMIN".equalsIgnoreCase(roleName)) {
            throw new ForbiddenException("Only Citizens, NGOs, and Admins can organize food drives.");
        }

        LocalDate eventDate = request.getEventDate() != null ? request.getEventDate() : LocalDate.now().plusDays(7);
        String startTime = request.getStartTime() != null ? request.getStartTime() : "10:00 AM";
        String endTime = request.getEndTime() != null ? request.getEndTime() : "05:00 PM";

        LocalDateTime deadline;
        if (request.getCompletionDeadline() != null) {
            deadline = request.getCompletionDeadline();
            if (deadline.isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Completion deadline cannot be in the past.");
            }
        } else {
            LocalTime endT = parseTime(endTime, LocalTime.of(23, 59));
            deadline = eventDate.atTime(endT);
        }

        FoodDrive drive = FoodDrive.builder()
                .createdByUser(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .purpose(request.getPurpose())
                .address(request.getAddress() != null ? request.getAddress() : "Address")
                .city(request.getCity())
                .state(request.getState() != null ? request.getState() : "State")
                .eventDate(eventDate)
                .startTime(startTime)
                .endTime(endTime)
                .coverImageUrl(request.getCoverImageUrl())
                .completionDeadline(deadline)
                .status("UPCOMING")
                .build();

        drive = foodDriveRepository.save(drive);
        refreshDriveStatus(drive);

        // Notify nearby NGOs, Restaurants, Volunteers in the city
        List<Ngo> nearbyNgos = ngoRepository.findByCityContainingIgnoreCase(drive.getCity());
        for (Ngo ngo : nearbyNgos) {
            notificationService.sendNotification(
                    ngo.getUser().getId(),
                    "New Food Drive Near You!",
                    "Food Drive '" + drive.getTitle() + "' announced in " + drive.getCity() + " on " + drive.getEventDate() + ".",
                    "FOOD_DRIVE_CREATED",
                    "FOOD_DRIVE",
                    drive.getId()
            );
        }

        List<Restaurant> nearbyRestaurants = restaurantRepository.findByCityContainingIgnoreCase(drive.getCity());
        for (Restaurant r : nearbyRestaurants) {
            notificationService.sendNotification(
                    r.getUser().getId(),
                    "New Food Drive Near You!",
                    "Food Drive '" + drive.getTitle() + "' announced in " + drive.getCity() + " on " + drive.getEventDate() + ". Would you like to contribute surplus food?",
                    "FOOD_DRIVE_CREATED",
                    "FOOD_DRIVE",
                    drive.getId()
            );
        }

        List<Volunteer> nearbyVolunteers = volunteerRepository.findByCityContainingIgnoreCaseAndAvailabilityStatus(drive.getCity(), "ACTIVE");
        for (Volunteer v : nearbyVolunteers) {
            notificationService.sendNotification(
                    v.getUser().getId(),
                    "Volunteer Opportunity: Food Drive",
                    "Food Drive '" + drive.getTitle() + "' needs volunteers in " + drive.getCity() + " on " + drive.getEventDate() + ".",
                    "FOOD_DRIVE_CREATED",
                    "FOOD_DRIVE",
                    drive.getId()
            );
        }

        return mapToResponse(drive, userId);
    }

    @Override
    @Transactional
    public List<FoodDriveResponse> getFoodDrives(String city, String status, Long currentUserId) {
        List<FoodDrive> drives;
        if (city != null && !city.isBlank()) {
            drives = foodDriveRepository.findByCityContainingIgnoreCase(city);
        } else {
            drives = foodDriveRepository.findAll();
        }

        for (FoodDrive d : drives) {
            refreshDriveStatus(d);
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            drives = drives.stream()
                    .filter(d -> d.getStatus() != null && d.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        return drives.stream().map(d -> mapToResponse(d, currentUserId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<FoodDriveResponse> getFoodDrives(String city, String status) {
        return getFoodDrives(city, status, null);
    }

    @Override
    @Transactional
    public List<FoodDriveResponse> getFoodDrives(String city) {
        return getFoodDrives(city, null, null);
    }

    @Override
    @Transactional
    public FoodDriveResponse getFoodDriveById(Long id, Long currentUserId) {
        FoodDrive drive = foodDriveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food Drive not found"));
        refreshDriveStatus(drive);
        return mapToResponse(drive, currentUserId);
    }

    @Override
    @Transactional
    public FoodDriveResponse getFoodDriveById(Long id) {
        return getFoodDriveById(id, null);
    }

    @Override
    @Transactional
    public FoodDriveParticipantResponse joinOrContribute(Long foodDriveId, Long userId, FoodDriveParticipantRequest request) {
        FoodDrive drive = foodDriveRepository.findById(foodDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Food Drive not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FoodDriveParticipant participant = participantRepository.findByFoodDriveIdAndUserId(foodDriveId, userId)
                .orElseGet(() -> FoodDriveParticipant.builder()
                        .foodDrive(drive)
                        .user(user)
                        .participantType(user.getRole() != null ? user.getRole().getName() : "USER")
                        .status("JOINED")
                        .build());

        if (request.getContributionDescription() != null) {
            participant.setContributionDescription(request.getContributionDescription());
        }
        if (request.getContributionQuantity() != null) {
            participant.setContributionQuantity(request.getContributionQuantity());
        }
        if (request.getContributionUnit() != null) {
            participant.setContributionUnit(request.getContributionUnit());
        }

        participant = participantRepository.save(participant);

        // Notify Host
        notificationService.sendNotification(
                drive.getCreatedByUser().getId(),
                "New Participant for Food Drive!",
                user.getName() + " (" + (user.getRole() != null ? user.getRole().getName() : "USER") + ") joined your food drive '" + drive.getTitle() + "' with contribution: " + participant.getContributionQuantity() + " " + participant.getContributionUnit(),
                "FOOD_DRIVE_UPDATE",
                "FOOD_DRIVE",
                drive.getId()
        );

        return mapToParticipantResponse(participant);
    }

    @Override
    @Transactional
    public List<FoodDriveResponse> getFoodDrivesByHost(Long userId) {
        List<FoodDrive> drives = foodDriveRepository.findByCreatedByUserId(userId);
        for (FoodDrive d : drives) {
            refreshDriveStatus(d);
        }
        return drives.stream().map(d -> mapToResponse(d, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FoodDriveResponse addEvidencePhoto(Long foodDriveId, Long userId, FoodDrivePhotoRequest request) {
        FoodDrive drive = foodDriveRepository.findById(foodDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Food Drive not found"));
        refreshDriveStatus(drive);

        if (!drive.getCreatedByUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the food drive creator can manage evidence photos.");
        }

        if ("COMPLETED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot modify evidence photos of an already completed food drive.");
        }
        if ("CANCELLED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot modify evidence photos of a cancelled food drive.");
        }
        if ("EXPIRED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot modify evidence photos of an expired food drive.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = getEventStart(drive);
        if (now.isBefore(eventStart)) {
            throw new BadRequestException("Cannot upload evidence photos before the food drive event has started.");
        }

        LocalDateTime deadline = getCompletionDeadline(drive);
        if (!now.isBefore(deadline)) {
            throw new BadRequestException("Cannot upload evidence photos after the completion deadline.");
        }

        List<String> photos = parsePhotos(drive.getEvidencePhotos());
        if (request != null && request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            for (String u : request.getPhotoUrls()) {
                if (u != null && !u.isBlank()) {
                    String clean = u.trim();
                    if (clean.contains("..")) {
                        throw new BadRequestException("Invalid photo URL: path traversal is not permitted.");
                    }
                    if (!photos.contains(clean)) {
                        photos.add(clean);
                    }
                }
            }
        }
        if (request != null && request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            String url = request.getPhotoUrl().trim();
            if (url.contains("..")) {
                throw new BadRequestException("Invalid photo URL: path traversal is not permitted.");
            }
            if (!photos.contains(url)) {
                photos.add(url);
            }
        }
        if (photos.isEmpty()) {
            throw new BadRequestException("Photo URL cannot be empty.");
        }
        drive.setEvidencePhotos(String.join(",", photos));
        drive = foodDriveRepository.save(drive);

        return mapToResponse(drive, userId);
    }

    @Override
    @Transactional
    public FoodDriveResponse removeEvidencePhoto(Long foodDriveId, Long userId, FoodDrivePhotoRequest request) {
        FoodDrive drive = foodDriveRepository.findById(foodDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Food Drive not found"));
        refreshDriveStatus(drive);

        if (!drive.getCreatedByUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the food drive creator can manage evidence photos.");
        }

        if ("COMPLETED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot modify evidence photos of an already completed food drive.");
        }
        if ("CANCELLED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot modify evidence photos of a cancelled food drive.");
        }
        if ("EXPIRED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot modify evidence photos of an expired food drive.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = getEventStart(drive);
        if (now.isBefore(eventStart)) {
            throw new BadRequestException("Cannot remove evidence photos before the food drive event has started.");
        }

        String url = request != null && request.getPhotoUrl() != null ? request.getPhotoUrl().trim() : "";
        List<String> photos = parsePhotos(drive.getEvidencePhotos());

        if (Boolean.TRUE.equals(drive.getPhotosSubmitted()) && (photos.size() - 1 < 4)) {
            throw new BadRequestException("Cannot remove photo: a minimum of 4 evidence photos must be maintained for a drive with submitted evidence. Upload a replacement before removing.");
        }

        photos.remove(url);
        drive.setEvidencePhotos(String.join(",", photos));
        drive = foodDriveRepository.save(drive);

        return mapToResponse(drive, userId);
    }

    @Override
    @Transactional
    public FoodDriveResponse submitEvidencePhotos(Long foodDriveId, Long userId, FoodDrivePhotoRequest request) {
        FoodDrive drive = foodDriveRepository.findById(foodDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Food Drive not found"));
        refreshDriveStatus(drive);

        if (!drive.getCreatedByUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the food drive creator can submit evidence photos.");
        }

        if ("COMPLETED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot submit photos for an already completed food drive.");
        }
        if ("CANCELLED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot submit photos for a cancelled food drive.");
        }
        if ("EXPIRED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cannot submit photos for an expired food drive.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = getEventStart(drive);
        if (now.isBefore(eventStart)) {
            throw new BadRequestException("Cannot submit evidence photos before the food drive event has started.");
        }

        LocalDateTime deadline = getCompletionDeadline(drive);
        if (!now.isBefore(deadline)) {
            throw new BadRequestException("Cannot submit evidence photos after the completion deadline.");
        }

        List<String> photosToSubmit;
        if (request != null && request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            photosToSubmit = request.getPhotoUrls();
        } else if (request != null && request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            photosToSubmit = List.of(request.getPhotoUrl());
        } else {
            photosToSubmit = parsePhotos(drive.getEvidencePhotos());
        }

        List<String> validPhotos = photosToSubmit.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        for (String p : validPhotos) {
            if (p.contains("..")) {
                throw new BadRequestException("Invalid photo URL: path traversal is not permitted.");
            }
        }

        if (validPhotos.size() < 4) {
            throw new BadRequestException("At least four valid evidence photos are required for submission. Current photos: " + validPhotos.size());
        }

        drive.setEvidencePhotos(String.join(",", validPhotos));
        drive.setPhotosSubmitted(true);
        drive.setPhotosSubmittedAt(LocalDateTime.now());
        drive = foodDriveRepository.save(drive);

        return mapToResponse(drive, userId);
    }

    @Override
    @Transactional
    public FoodDriveResponse completeFoodDrive(Long foodDriveId, Long userId) {
        FoodDrive drive = foodDriveRepository.findById(foodDriveId)
                .orElseThrow(() -> new ResourceNotFoundException("Food Drive not found"));
        refreshDriveStatus(drive);

        // 1. Authenticated user must be creator
        if (!drive.getCreatedByUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the food drive creator can complete this drive.");
        }

        // Idempotency: if already completed, return successfully
        if ("COMPLETED".equalsIgnoreCase(drive.getStatus())) {
            return mapToResponse(drive, userId);
        }

        if ("CANCELLED".equalsIgnoreCase(drive.getStatus())) {
            throw new BadRequestException("Cancelled food drive cannot be completed.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = getEventStart(drive);
        if (now.isBefore(eventStart)) {
            throw new BadRequestException("Cannot complete food drive before the event has started.");
        }

        // 2. Drive completion deadline check: must complete before the deadline
        LocalDateTime deadline = getCompletionDeadline(drive);
        if ("EXPIRED".equalsIgnoreCase(drive.getStatus()) || !now.isBefore(deadline)) {
            if (!"EXPIRED".equalsIgnoreCase(drive.getStatus())) {
                drive.setStatus("EXPIRED");
                foodDriveRepository.save(drive);
            }
            throw new BadRequestException("Completion deadline has passed. Drive has expired and cannot be completed.");
        }

        // 3. Minimum four valid distinct evidence photos
        List<String> photos = parsePhotos(drive.getEvidencePhotos());
        if (photos.size() < 4) {
            throw new BadRequestException("At least four valid evidence photos are required to complete the food drive. Current photos: " + photos.size());
        }

        // 4. Evidence photos must be submitted before completion
        if (!Boolean.TRUE.equals(drive.getPhotosSubmitted())) {
            throw new BadRequestException("Evidence photos must be submitted before the food drive can be marked as completed.");
        }

        // Transition to COMPLETED
        drive.setStatus("COMPLETED");
        drive.setCompletedAt(LocalDateTime.now());
        drive = foodDriveRepository.save(drive);

        // Notify participants
        List<FoodDriveParticipant> participants = participantRepository.findByFoodDriveId(drive.getId());
        for (FoodDriveParticipant p : participants) {
            if (!p.getUser().getId().equals(userId)) {
                notificationService.sendNotification(
                        p.getUser().getId(),
                        "Food Drive Completed!",
                        "Food Drive '" + drive.getTitle() + "' has been marked as Completed! Check out the completion photo gallery.",
                        "FOOD_DRIVE_COMPLETED",
                        "FOOD_DRIVE",
                        drive.getId()
                );
            }
        }

        // Notify host
        notificationService.sendNotification(
                userId,
                "Food Drive Completed Successfully!",
                "Your food drive '" + drive.getTitle() + "' has been verified and marked as Completed with " + photos.size() + " evidence photos.",
                "FOOD_DRIVE_COMPLETED",
                "FOOD_DRIVE",
                drive.getId()
        );

        return mapToResponse(drive, userId);
    }

    private LocalDateTime getEventStart(FoodDrive drive) {
        LocalDate eventDate = drive.getEventDate() != null ? drive.getEventDate() : LocalDate.now();
        LocalTime startTime = parseTime(drive.getStartTime(), LocalTime.of(9, 0));
        return eventDate.atTime(startTime);
    }

    private LocalDateTime getCompletionDeadline(FoodDrive drive) {
        LocalDateTime deadline = drive.getCompletionDeadline();
        if (deadline == null) {
            LocalDate eventDate = drive.getEventDate() != null ? drive.getEventDate() : LocalDate.now();
            LocalTime endT = parseTime(drive.getEndTime(), LocalTime.of(23, 59));
            deadline = eventDate.atTime(endT);
        }
        return deadline;
    }

    private void refreshDriveStatus(FoodDrive drive) {
        if (drive == null) return;
        if ("COMPLETED".equalsIgnoreCase(drive.getStatus()) || "CANCELLED".equalsIgnoreCase(drive.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = getEventStart(drive);
        LocalDateTime deadline = getCompletionDeadline(drive);

        if (!now.isBefore(deadline)) {
            if (!"EXPIRED".equalsIgnoreCase(drive.getStatus())) {
                drive.setStatus("EXPIRED");
                foodDriveRepository.save(drive);
            }
        } else if (now.isAfter(eventStart) || now.isEqual(eventStart)) {
            if (!"IN_PROGRESS".equalsIgnoreCase(drive.getStatus())) {
                drive.setStatus("IN_PROGRESS");
                foodDriveRepository.save(drive);
            }
        } else {
            if (!"UPCOMING".equalsIgnoreCase(drive.getStatus())) {
                drive.setStatus("UPCOMING");
                foodDriveRepository.save(drive);
            }
        }
    }

    private LocalTime parseTime(String timeStr, LocalTime defaultTime) {
        if (timeStr == null || timeStr.isBlank()) {
            return defaultTime;
        }
        String clean = timeStr.trim();
        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm", "H:mm", "hh:mma", "h:mma"};
        for (String pattern : patterns) {
            try {
                return LocalTime.parse(clean, DateTimeFormatter.ofPattern(pattern, java.util.Locale.ENGLISH));
            } catch (DateTimeParseException ignored) {}
        }
        return defaultTime;
    }

    private List<String> parsePhotos(String evidencePhotos) {
        if (evidencePhotos == null || evidencePhotos.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(evidencePhotos.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private FoodDriveResponse mapToResponse(FoodDrive d, Long currentUserId) {
        List<FoodDriveParticipant> participants = participantRepository.findByFoodDriveId(d.getId());
        int totalMeals = participants.stream()
                .mapToInt(p -> p.getContributionQuantity() != null ? p.getContributionQuantity() : 0)
                .sum();

        List<FoodDriveParticipantResponse> pResponses = participants.stream()
                .map(this::mapToParticipantResponse)
                .collect(Collectors.toList());

        boolean isCreator = currentUserId != null && d.getCreatedByUser() != null && currentUserId.equals(d.getCreatedByUser().getId());

        List<String> photos = parsePhotos(d.getEvidencePhotos());
        List<String> visiblePhotos;
        if (isCreator || "COMPLETED".equalsIgnoreCase(d.getStatus())) {
            visiblePhotos = photos;
        } else {
            visiblePhotos = Collections.emptyList();
        }

        boolean photosSubmitted = Boolean.TRUE.equals(d.getPhotosSubmitted()) || "COMPLETED".equalsIgnoreCase(d.getStatus());

        return FoodDriveResponse.builder()
                .id(d.getId())
                .createdByUserId(d.getCreatedByUser().getId())
                .createdByName(d.getCreatedByUser().getName())
                .createdByRole(d.getCreatedByUser().getRole() != null ? d.getCreatedByUser().getRole().getName() : "USER")
                .title(d.getTitle())
                .description(d.getDescription())
                .purpose(d.getPurpose())
                .address(d.getAddress())
                .city(d.getCity())
                .state(d.getState())
                .eventDate(d.getEventDate())
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .coverImageUrl(d.getCoverImageUrl())
                .completionDeadline(d.getCompletionDeadline())
                .completedAt(d.getCompletedAt())
                .status(d.getStatus())
                .evidencePhotos(visiblePhotos)
                .isCreator(isCreator)
                .photosSubmitted(photosSubmitted)
                .photosSubmittedAt(d.getPhotosSubmittedAt())
                .participantCount(participants.size())
                .totalMealsContributed(totalMeals)
                .participants(pResponses)
                .createdAt(d.getCreatedAt())
                .build();
    }

    private FoodDriveParticipantResponse mapToParticipantResponse(FoodDriveParticipant p) {
        return FoodDriveParticipantResponse.builder()
                .id(p.getId())
                .foodDriveId(p.getFoodDrive().getId())
                .userId(p.getUser().getId())
                .userName(p.getUser().getName())
                .participantType(p.getParticipantType())
                .contributionDescription(p.getContributionDescription())
                .contributionQuantity(p.getContributionQuantity())
                .contributionUnit(p.getContributionUnit())
                .status(p.getStatus())
                .joinedAt(p.getJoinedAt())
                .build();
    }
}
