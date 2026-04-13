package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "selection_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(nullable = false)
    private String name; // e.g., "Bebas Testing", "Bebas Testing Syarat Ranking", "Testing"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "require_ranking")
    private Boolean requireRanking = false;

    @Column(name = "require_testing")
    private Boolean requireTesting = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormType formType;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    public enum FormType {
        MEDICAL,      // Kedokteran
        NON_MEDICAL   // Non-Kedokteran
    }
}
