package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_links")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamLink {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "selection_type_id", nullable = true) // null = general exam link
    private SelectionType selectionType;

    @Column(nullable = false)
    private String linkTitle; // e.g., "Link Ujian Lanjutan", "Link Ujian", "Link Google Form Ujian"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String linkUrl; // Google Form URL or other exam platform link

    @Column(columnDefinition = "TEXT")
    private String description;

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
}
