package com.ngoconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", nullable = false, unique = true)
    private Donation donation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ngo_id", nullable = false)
    private Ngo ngo;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(length = 20)
    @Builder.Default
    private String status = "CLAIMED"; // CLAIMED, HANDED_OVER, CANCELLED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        claimedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }
}
