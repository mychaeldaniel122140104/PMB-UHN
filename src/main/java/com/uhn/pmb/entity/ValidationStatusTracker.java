package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * Entity untuk tracking status validasi user (Single Source of Truth)
 * Status berubah-ubah berdasarkan action admin:
 * - MENUNGGU: Sudah bayar, menunggu validasi
 * - DIVALIDASI: Sudah di-approve oleh validasi
 * - DITOLAK: Ditolak oleh validasi
 * - REVISI: Diminta revisi oleh validasi
 */
@Entity
@Table(name = "validation_status_tracker")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationStatusTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_form_id", nullable = false, unique = true)
    @JsonIgnore
    private AdmissionForm admissionForm;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatusEnum status = ValidationStatusEnum.NOT_STARTED;

    @Column(columnDefinition = "TEXT")
    private String lastReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by")
    @JsonIgnore
    private User lastUpdatedBy;

    @Column(name = "last_action")
    private String lastAction; // APPROVED, REJECTED, REVISION_REQUESTED, PAYMENT_VERIFIED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ValidationStatusEnum {
        NOT_STARTED,    // Belum ada aktivitas
        MENUNGGU,       // Sudah bayar, menunggu validasi
        DIVALIDASI,     // Sudah di-approve
        DITOLAK,        // Ditolak
        REVISI          // Diminta revisi
    }
}
