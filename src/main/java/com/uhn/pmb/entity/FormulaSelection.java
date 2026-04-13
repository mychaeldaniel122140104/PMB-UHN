package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FormulaSelection represents the formula/selection options available to students
 * (e.g., Kedokteran vs Non-Kedokteran)
 * This is independent from SelectionType and RegistrationPeriod
 */
@Entity
@Table(name = "formula_selections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormulaSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g., "MEDICAL", "NON_MEDICAL"

    @Column(nullable = false)
    private String title; // e.g., "Kedokteran", "Program Non-Kedokteran"

    @Column(columnDefinition = "TEXT")
    private String description; // Detailed description

    @Column(columnDefinition = "TEXT")
    private String features; // JSON or comma-separated features

    @Column(name = "icon_emoji")
    private String iconEmoji; // e.g., "🏥", "📚"

    @Column(nullable = false)
    private BigDecimal price; // Registration fee

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SelectionType.FormType formType;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

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
}
