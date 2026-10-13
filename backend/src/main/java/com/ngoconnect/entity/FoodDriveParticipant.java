package com.ngoconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_drive_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDriveParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_drive_id", nullable = false)
    private FoodDrive foodDrive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "participant_type", nullable = false, length = 50)
    private String participantType; // NGO, RESTAURANT, VOLUNTEER

    @Column(name = "contribution_description", columnDefinition = "TEXT")
    private String contributionDescription;

    @Column(name = "contribution_quantity")
    @Builder.Default
    private Integer contributionQuantity = 0;

    @Column(name = "contribution_unit", length = 30)
    @Builder.Default
    private String contributionUnit = "meals";

    @Column(length = 20)
    @Builder.Default
    private String status = "JOINED"; // JOINED, CONFIRMED, CANCELLED

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
