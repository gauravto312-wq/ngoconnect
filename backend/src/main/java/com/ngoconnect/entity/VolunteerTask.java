package com.ngoconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private Volunteer volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id")
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_drive_id")
    private FoodDrive foodDrive;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType; // PICKUP_DONATION, FOOD_DRIVE_HELP

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(length = 20)
    @Builder.Default
    private String status = "ASSIGNED"; // ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, REJECTED

    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    @Column(name = "payment_amount", precision = 10, scale = 2)
    private BigDecimal paymentAmount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "payment_terms", length = 255)
    private String paymentTerms;

    @Column(name = "payment_status", length = 20)
    @Builder.Default
    private String paymentStatus = "NOT_APPLICABLE"; // NOT_APPLICABLE, PENDING, PAID, DISPUTED

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by_user_id")
    private Long paidByUserId;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
