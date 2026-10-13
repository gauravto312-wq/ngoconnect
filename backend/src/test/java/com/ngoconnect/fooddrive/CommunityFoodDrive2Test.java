package com.ngoconnect.fooddrive;

import com.ngoconnect.dto.*;
import com.ngoconnect.entity.FoodDrive;
import com.ngoconnect.exception.BadRequestException;
import com.ngoconnect.exception.ForbiddenException;
import com.ngoconnect.repository.FoodDriveRepository;
import com.ngoconnect.service.AuthService;
import com.ngoconnect.service.FoodDriveService;
import com.ngoconnect.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CommunityFoodDrive2Test {

    @Autowired
    private AuthService authService;

    @Autowired
    private FoodDriveService foodDriveService;

    @Autowired
    private FoodDriveRepository foodDriveRepository;

    @Autowired
    private NotificationService notificationService;

    private AuthResponse citizenHost;
    private AuthResponse volunteerParticipant;
    private AuthResponse unrelatedUser;

    @BeforeEach
    void setUp() {
        RegisterRequest hostReq = new RegisterRequest();
        hostReq.setName("Pooja Sharma");
        hostReq.setEmail("pooja.citizen@ngoconnect.org");
        hostReq.setPhone("9876500001");
        hostReq.setPassword("Password123!");
        hostReq.setRole("CITIZEN");
        hostReq.setCity("Lucknow");
        citizenHost = authService.register(hostReq);

        RegisterRequest partReq = new RegisterRequest();
        partReq.setName("Aman Volunteer");
        partReq.setEmail("aman.volunteer@ngoconnect.org");
        partReq.setPhone("9876500002");
        partReq.setPassword("Password123!");
        partReq.setRole("VOLUNTEER");
        partReq.setCity("Lucknow");
        volunteerParticipant = authService.register(partReq);

        RegisterRequest unrelatedReq = new RegisterRequest();
        unrelatedReq.setName("Vikram Unrelated");
        unrelatedReq.setEmail("vikram.other@ngoconnect.org");
        unrelatedReq.setPhone("9876500003");
        unrelatedReq.setPassword("Password123!");
        unrelatedReq.setRole("CITIZEN");
        unrelatedReq.setCity("Lucknow");
        unrelatedUser = authService.register(unrelatedReq);
    }

    @Test
    @DisplayName("1. shouldCreateFoodDriveWithDeadlineAndCoverImage")
    void shouldCreateFoodDriveWithDeadlineAndCoverImage() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("BBD Gomti Nagar Relief Drive");
        req.setPurpose("Providing nutritious meals to migrant workers");
        req.setDescription("Weekend distribution camp near stadium");
        req.setAddress("Vibhuti Khand, Gomti Nagar");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        req.setEventDate(LocalDate.now().plusDays(2));
        req.setStartTime("10:00 AM");
        req.setEndTime("02:00 PM");
        req.setCompletionDeadline(LocalDateTime.now().plusDays(2).plusHours(5));
        req.setCoverImageUrl("/uploads/relief-banner.jpg");

        FoodDriveResponse resp = foodDriveService.createFoodDrive(citizenHost.getId(), req);

        assertNotNull(resp);
        assertNotNull(resp.getId());
        assertEquals("UPCOMING", resp.getStatus());
        assertEquals("/uploads/relief-banner.jpg", resp.getCoverImageUrl());
        assertNotNull(resp.getCompletionDeadline());
        assertTrue(resp.getIsCreator());
    }

    @Test
    @DisplayName("2. shouldRejectFoodDriveCreationWithPastDeadline")
    void shouldRejectFoodDriveCreationWithPastDeadline() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Past Deadline Drive");
        req.setPurpose("Testing past deadline rejection");
        req.setAddress("Indira Nagar");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        req.setEventDate(LocalDate.now().minusDays(1));
        req.setStartTime("10:00 AM");
        req.setEndTime("02:00 PM");
        req.setCompletionDeadline(LocalDateTime.now().minusHours(2));

        assertThrows(BadRequestException.class, () ->
                foodDriveService.createFoodDrive(citizenHost.getId(), req)
        );
    }

    @Test
    @DisplayName("3. shouldAllowCreatorToUploadEvidencePhotos")
    void shouldAllowCreatorToUploadEvidencePhotos() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        FoodDriveResponse updated = foodDriveService.addEvidencePhoto(
                drive.getId(),
                citizenHost.getId(),
                new FoodDrivePhotoRequest("/uploads/photo1.jpg")
        );

        assertNotNull(updated);
        assertEquals(1, updated.getEvidencePhotos().size());
        assertEquals("/uploads/photo1.jpg", updated.getEvidencePhotos().get(0));
    }

    @Test
    @DisplayName("4. shouldPreventNonCreatorFromUploadingEvidencePhotos")
    void shouldPreventNonCreatorFromUploadingEvidencePhotos() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        assertThrows(ForbiddenException.class, () ->
                foodDriveService.addEvidencePhoto(
                        drive.getId(),
                        unrelatedUser.getId(),
                        new FoodDrivePhotoRequest("/uploads/unauthorized.jpg")
                )
        );
    }

    @Test
    @DisplayName("5. shouldPreventPhotoUploadWithTraversalOrUnsafeUrl")
    void shouldPreventPhotoUploadWithTraversalOrUnsafeUrl() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        assertThrows(BadRequestException.class, () ->
                foodDriveService.addEvidencePhoto(
                        drive.getId(),
                        citizenHost.getId(),
                        new FoodDrivePhotoRequest("../../etc/passwd")
                )
        );
    }

    @Test
    @DisplayName("6. shouldAllowCreatorToRemoveEvidencePhoto")
    void shouldAllowCreatorToRemoveEvidencePhoto() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/photo1.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/photo2.jpg"));

        FoodDriveResponse afterRemove = foodDriveService.removeEvidencePhoto(
                drive.getId(),
                citizenHost.getId(),
                new FoodDrivePhotoRequest("/uploads/photo1.jpg")
        );

        assertEquals(1, afterRemove.getEvidencePhotos().size());
        assertEquals("/uploads/photo2.jpg", afterRemove.getEvidencePhotos().get(0));
    }

    @Test
    @DisplayName("7. shouldRejectCompletionAfterDeadlinePassed")
    void shouldRejectCompletionAfterDeadlinePassed() {
        // Create active drive and submit 4 photos while active
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);

        // Now deadline expires
        FoodDrive entity = foodDriveRepository.findById(drive.getId()).orElseThrow();
        entity.setCompletionDeadline(LocalDateTime.now().minusSeconds(10));
        foodDriveRepository.save(entity);

        // Attempt completion after deadline has passed
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId())
        );

        assertTrue(ex.getMessage().contains("Completion deadline has passed") || ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("8. shouldRejectCompletionWithFewerThanFourPhotos")
    void shouldRejectCompletionWithFewerThanFourPhotos() {
        // Create active drive with deadline in the future
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        // Upload only 3 photos
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p1.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p2.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p3.jpg"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId())
        );

        assertTrue(ex.getMessage().contains("At least four valid evidence photos") || ex.getMessage().contains("must be submitted"));
    }

    @Test
    @DisplayName("9. shouldRejectCompletionByNonCreator")
    void shouldRejectCompletionByNonCreator() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);

        assertThrows(ForbiddenException.class, () ->
                foodDriveService.completeFoodDrive(drive.getId(), unrelatedUser.getId())
        );
    }

    @Test
    @DisplayName("10. shouldCompleteFoodDriveBeforeDeadlineWithFourPhotos")
    void shouldCompleteFoodDriveBeforeDeadlineWithFourPhotos() {
        // Drive with deadline in the future: creator can complete before deadline
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        // Join participant
        FoodDriveParticipantRequest partReq = new FoodDriveParticipantRequest();
        partReq.setContributionDescription("Meal Packets");
        partReq.setContributionQuantity(50);
        foodDriveService.joinOrContribute(drive.getId(), volunteerParticipant.getId(), partReq);

        // Upload & submit 4 photos
        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);

        // Complete drive before deadline
        FoodDriveResponse completed = foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId());

        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(4, completed.getEvidencePhotos().size());

        // Verify participant received in-app notification
        List<NotificationResponse> notifs = notificationService.getUserNotifications(volunteerParticipant.getId());
        assertTrue(notifs.stream().anyMatch(n -> n.getTitle().contains("Food Drive Completed")));
    }

    @Test
    @DisplayName("11. shouldBeIdempotentOnDuplicateCompletionRequests")
    void shouldBeIdempotentOnDuplicateCompletionRequests() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);

        FoodDriveResponse first = foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId());
        FoodDriveResponse duplicate = foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId());

        assertEquals("COMPLETED", first.getStatus());
        assertEquals("COMPLETED", duplicate.getStatus());
        assertEquals(first.getId(), duplicate.getId());
    }

    @Test
    @DisplayName("12. shouldTransitionStatusToExpiredWhenDeadlinePassedWithoutCompletion")
    void shouldTransitionStatusToExpiredWhenDeadlinePassedWithoutCompletion() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().minusSeconds(5));

        // When retrieved without completion, status should dynamically evaluate to EXPIRED
        FoodDriveResponse fetched = foodDriveService.getFoodDriveById(drive.getId(), null);
        assertEquals("EXPIRED", fetched.getStatus());
    }

    @Test
    @DisplayName("13. shouldExposeEvidencePhotosPubliclyOnlyForCompletedDrives")
    void shouldExposeEvidencePhotosPubliclyOnlyForCompletedDrives() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p1.jpg"));

        // Unrelated public visitor fetching active drive -> photos must be empty list
        FoodDriveResponse publicView = foodDriveService.getFoodDriveById(drive.getId(), null);
        assertTrue(publicView.getEvidencePhotos().isEmpty());

        // Creator fetching active drive -> can see their photos
        FoodDriveResponse creatorView = foodDriveService.getFoodDriveById(drive.getId(), citizenHost.getId());
        assertEquals(1, creatorView.getEvidencePhotos().size());
    }

    @Test
    @DisplayName("14. shouldFilterFoodDrivesByStatus")
    void shouldFilterFoodDrivesByStatus() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Upcoming Food Drive Filter Test");
        req.setAddress("Gomti Nagar");
        req.setCity("Lucknow");
        req.setEventDate(LocalDate.now().plusDays(5));
        req.setStartTime("10:00 AM");
        req.setEndTime("06:00 PM");
        req.setCompletionDeadline(LocalDateTime.now().plusDays(5).plusHours(2));
        FoodDriveResponse upcoming = foodDriveService.createFoodDrive(citizenHost.getId(), req);

        List<FoodDriveResponse> upcomingList = foodDriveService.getFoodDrives(null, "UPCOMING");
        assertTrue(upcomingList.stream().anyMatch(d -> d.getId().equals(upcoming.getId())));

        List<FoodDriveResponse> completedList = foodDriveService.getFoodDrives(null, "COMPLETED");
        assertFalse(completedList.stream().anyMatch(d -> d.getId().equals(upcoming.getId())));
    }

    @Test
    @DisplayName("15. shouldPreserveBackwardCompatibilityForExistingDrivesWithoutDeadline")
    void shouldPreserveBackwardCompatibilityForExistingDrivesWithoutDeadline() {
        // Drive created with null deadline falls back to end of eventDate
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Legacy Style Drive");
        req.setAddress("Aliganj Sector C");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        req.setEventDate(LocalDate.now().plusDays(10));
        req.setStartTime("10:00 AM");
        req.setEndTime("06:00 PM");
        req.setCompletionDeadline(null);

        FoodDriveResponse resp = foodDriveService.createFoodDrive(citizenHost.getId(), req);
        assertNotNull(resp);
        assertEquals("UPCOMING", resp.getStatus());
        assertNotNull(resp.getCompletionDeadline());
    }

    @Test
    @DisplayName("16. shouldPreserveCreatorPhotosInListWhileHidingFromOthersUntilCompleted")
    void shouldPreserveCreatorPhotosInListWhileHidingFromOthersUntilCompleted() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusDays(1));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/creator-evidence.jpg"));

        // Host queries the list -> evidence photos visible, isCreator = true
        List<FoodDriveResponse> hostList = foodDriveService.getFoodDrives(null, null, citizenHost.getId());
        FoodDriveResponse hostDrive = hostList.stream().filter(d -> d.getId().equals(drive.getId())).findFirst().orElseThrow();
        assertTrue(hostDrive.getIsCreator());
        assertEquals(1, hostDrive.getEvidencePhotos().size());
        assertEquals("/uploads/creator-evidence.jpg", hostDrive.getEvidencePhotos().get(0));

        // Other user queries the list -> photos hidden, isCreator = false
        List<FoodDriveResponse> otherList = foodDriveService.getFoodDrives(null, null, unrelatedUser.getId());
        FoodDriveResponse otherDrive = otherList.stream().filter(d -> d.getId().equals(drive.getId())).findFirst().orElseThrow();
        assertFalse(otherDrive.getIsCreator());
        assertTrue(otherDrive.getEvidencePhotos().isEmpty());

        // Anonymous public visitor queries the list -> photos hidden, isCreator = false
        List<FoodDriveResponse> publicList = foodDriveService.getFoodDrives(null, null, null);
        FoodDriveResponse publicDrive = publicList.stream().filter(d -> d.getId().equals(drive.getId())).findFirst().orElseThrow();
        assertFalse(publicDrive.getIsCreator());
        assertTrue(publicDrive.getEvidencePhotos().isEmpty());
    }

    @Test
    @DisplayName("17. shouldRejectPhotoSubmissionWithFewerThanFourPhotos")
    void shouldRejectPhotoSubmissionWithFewerThanFourPhotos() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusDays(1));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p1.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p2.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p3.jpg"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), null)
        );
        assertTrue(ex.getMessage().contains("At least four valid evidence photos are required"));
    }

    @Test
    @DisplayName("18. shouldSubmitEvidencePhotosSuccessfullyWithFourOrMorePhotos")
    void shouldSubmitEvidencePhotosSuccessfullyWithFourOrMorePhotos() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusDays(1));
        FoodDrivePhotoRequest req = new FoodDrivePhotoRequest();
        req.setPhotoUrls(List.of("/uploads/s1.jpg", "/uploads/s2.jpg", "/uploads/s3.jpg", "/uploads/s4.jpg"));

        FoodDriveResponse submitted = foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), req);

        assertNotNull(submitted);
        assertTrue(submitted.getPhotosSubmitted());
        assertEquals(4, submitted.getEvidencePhotos().size());
    }

    @Test
    @DisplayName("19. shouldRejectPhotoSubmissionByNonCreator")
    void shouldRejectPhotoSubmissionByNonCreator() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusDays(1));
        FoodDrivePhotoRequest req = new FoodDrivePhotoRequest();
        req.setPhotoUrls(List.of("/uploads/s1.jpg", "/uploads/s2.jpg", "/uploads/s3.jpg", "/uploads/s4.jpg"));

        assertThrows(ForbiddenException.class, () ->
                foodDriveService.submitEvidencePhotos(drive.getId(), unrelatedUser.getId(), req)
        );
    }

    @Test
    @DisplayName("20. shouldPersistSubmittedPhotosAcrossRefetches")
    void shouldPersistSubmittedPhotosAcrossRefetches() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusDays(1));
        FoodDrivePhotoRequest req = new FoodDrivePhotoRequest();
        req.setPhotoUrls(List.of("/uploads/sub1.jpg", "/uploads/sub2.jpg", "/uploads/sub3.jpg", "/uploads/sub4.jpg", "/uploads/sub5.jpg"));

        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), req);

        // Refetch drive as creator
        FoodDriveResponse refetched = foodDriveService.getFoodDriveById(drive.getId(), citizenHost.getId());
        assertTrue(refetched.getPhotosSubmitted());
        assertEquals(5, refetched.getEvidencePhotos().size());

        // List query as creator
        List<FoodDriveResponse> hostList = foodDriveService.getFoodDrives(null, null, citizenHost.getId());
        FoodDriveResponse hostDrive = hostList.stream().filter(d -> d.getId().equals(drive.getId())).findFirst().orElseThrow();
        assertTrue(hostDrive.getPhotosSubmitted());
        assertEquals(5, hostDrive.getEvidencePhotos().size());
    }

    @Test
    @DisplayName("21. shouldRejectEvidencePhotoModificationOnExpiredDrive")
    void shouldRejectEvidencePhotoModificationOnExpiredDrive() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().minusSeconds(10));

        assertThrows(BadRequestException.class, () ->
                foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/too-late.jpg"))
        );

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        assertThrows(BadRequestException.class, () ->
                foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq)
        );
    }

    @Test
    @DisplayName("22. shouldAllowCompletionImmediatelyAfterSubmittingPhotosBeforeDeadline")
    void shouldAllowCompletionImmediatelyAfterSubmittingPhotosBeforeDeadline() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(3));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/sub1.jpg", "/uploads/sub2.jpg", "/uploads/sub3.jpg", "/uploads/sub4.jpg"));
        FoodDriveResponse submitted = foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);
        assertTrue(submitted.getPhotosSubmitted());

        FoodDriveResponse completed = foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId());
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(4, completed.getEvidencePhotos().size());
    }

    @Test
    @DisplayName("23. shouldBlockPhotoUploadAndCompletionBeforeDriveStarts")
    void shouldBlockPhotoUploadAndCompletionBeforeDriveStarts() {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Future Unstarted Drive");
        req.setAddress("Aliganj Sector B");
        req.setCity("Lucknow");
        req.setEventDate(LocalDate.now().plusDays(2));
        req.setStartTime("10:00 AM");
        req.setEndTime("06:00 PM");
        req.setCompletionDeadline(LocalDateTime.now().plusDays(2).plusHours(4));
        FoodDriveResponse futureDrive = foodDriveService.createFoodDrive(citizenHost.getId(), req);
        assertEquals("UPCOMING", futureDrive.getStatus());

        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                foodDriveService.addEvidencePhoto(futureDrive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/early.jpg"))
        );
        assertTrue(ex1.getMessage().contains("before the food drive event has started"));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                foodDriveService.submitEvidencePhotos(futureDrive.getId(), citizenHost.getId(), subReq)
        );
        assertTrue(ex2.getMessage().contains("before the food drive event has started"));

        BadRequestException ex3 = assertThrows(BadRequestException.class, () ->
                foodDriveService.completeFoodDrive(futureDrive.getId(), citizenHost.getId())
        );
        assertTrue(ex3.getMessage().contains("before the event has started"));
    }

    @Test
    @DisplayName("24. shouldRejectCompletionIfEvidencePhotosNotSubmitted")
    void shouldRejectCompletionIfEvidencePhotosNotSubmitted() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        // Add 4 photos without calling submitEvidencePhotos
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p1.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p2.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p3.jpg"));
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p4.jpg"));

        // Attempt completion
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId())
        );
        assertTrue(ex.getMessage().contains("must be submitted"));
    }

    @Test
    @DisplayName("25. shouldNeverExpireCompletedDriveAfterDeadlinePasses")
    void shouldNeverExpireCompletedDriveAfterDeadlinePasses() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/c1.jpg", "/uploads/c2.jpg", "/uploads/c3.jpg", "/uploads/c4.jpg"));
        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);

        FoodDriveResponse completed = foodDriveService.completeFoodDrive(drive.getId(), citizenHost.getId());
        assertEquals("COMPLETED", completed.getStatus());

        // Fast-forward deadline into past
        FoodDrive entity = foodDriveRepository.findById(drive.getId()).orElseThrow();
        entity.setCompletionDeadline(LocalDateTime.now().minusHours(5));
        foodDriveRepository.save(entity);

        // Fetching drive must remain COMPLETED
        FoodDriveResponse fetched = foodDriveService.getFoodDriveById(drive.getId(), null);
        assertEquals("COMPLETED", fetched.getStatus());
    }

    @Test
    @DisplayName("26. shouldRejectPhotoRemovalIfDroppingBelowFourPhotosWhenSubmitted")
    void shouldRejectPhotoRemovalIfDroppingBelowFourPhotosWhenSubmitted() {
        FoodDriveResponse drive = createTestDrive(citizenHost.getId(), LocalDateTime.now().plusHours(2));

        FoodDrivePhotoRequest subReq = new FoodDrivePhotoRequest();
        subReq.setPhotoUrls(List.of("/uploads/p1.jpg", "/uploads/p2.jpg", "/uploads/p3.jpg", "/uploads/p4.jpg"));
        foodDriveService.submitEvidencePhotos(drive.getId(), citizenHost.getId(), subReq);

        // Attempting to remove 1 photo from submitted set of 4 must be rejected
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                foodDriveService.removeEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p1.jpg"))
        );
        assertTrue(ex.getMessage().contains("minimum of 4 evidence photos"));

        // Adding 5th photo allows removal of 1 photo while keeping 4
        foodDriveService.addEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p5.jpg"));
        FoodDriveResponse afterRemove = foodDriveService.removeEvidencePhoto(drive.getId(), citizenHost.getId(), new FoodDrivePhotoRequest("/uploads/p1.jpg"));
        assertEquals(4, afterRemove.getEvidencePhotos().size());
        assertTrue(afterRemove.getPhotosSubmitted());
    }

    private FoodDriveResponse createTestDrive(Long creatorId, LocalDateTime deadline) {
        FoodDriveRequest req = new FoodDriveRequest();
        req.setTitle("Community Food Drive for Testing");
        req.setPurpose("Relief efforts");
        req.setAddress("Kapoorthala, Aliganj");
        req.setCity("Lucknow");
        req.setState("Uttar Pradesh");
        req.setEventDate(LocalDate.now());
        req.setStartTime("06:00 AM");
        req.setEndTime("08:00 PM");
        req.setCompletionDeadline(LocalDateTime.now().plusHours(4));
        FoodDriveResponse created = foodDriveService.createFoodDrive(creatorId, req);
        if (deadline != null) {
            FoodDrive entity = foodDriveRepository.findById(created.getId()).orElseThrow();
            entity.setCompletionDeadline(deadline);
            foodDriveRepository.save(entity);
            return foodDriveService.getFoodDriveById(created.getId(), creatorId);
        }
        return created;
    }
}
