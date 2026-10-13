package com.ngoconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_user_id", nullable = false)
    private User donorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_branch_id")
    private RestaurantBranch restaurantBranch;

    @Column(nullable = false, length = 50)
    private String category; // FOOD, CLOTHES, OTHER

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 30)
    @Builder.Default
    private String unit = "packets";

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "availability_start")
    private LocalDateTime availabilityStart;

    @Column(name = "availability_end")
    private LocalDateTime availabilityEnd;

    @Column(name = "pickup_code", nullable = false, length = 20)
    private String pickupCode; // e.g. NGC-847291 (Private code)

    @Column(length = 20)
    @Builder.Default
    private String status = "AVAILABLE"; // AVAILABLE, CLAIMED, PICKUP, COMPLETED, EXPIRED

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

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
