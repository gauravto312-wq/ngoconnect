package com.ngoconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_drives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String purpose;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false, length = 20)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 20)
    private String endTime;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "completion_deadline")
    private LocalDateTime completionDeadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "evidence_photos", columnDefinition = "TEXT")
    private String evidencePhotos;

    @Column(name = "photos_submitted")
    @Builder.Default
    private Boolean photosSubmitted = false;

    @Column(name = "photos_submitted_at")
    private LocalDateTime photosSubmittedAt;

    @Column(length = 20)
    @Builder.Default
    private String status = "UPCOMING"; // UPCOMING, IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
