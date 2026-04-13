package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * Entity untuk tracking validasi data daftar ulang oleh admin
 */
@Entity
@Table(name = "reenrollment_validations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReEnrollmentValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reenrollment_id", nullable = false)
    @JsonIgnore
    private ReEnrollment reEnrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    private User validatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String rejectionTopic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    public enum ValidationStatus {
        PENDING,      // Belum divalidasi
        APPROVED,     // Disetujui
        REJECTED,     // Ditolak
        SUSPENDED     // Ditangguhkan
    }
}
