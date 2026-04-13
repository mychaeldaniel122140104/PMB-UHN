package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reenrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_result_id", nullable = true)  // ✅ CHANGED: Made nullable for non-exam students
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ===== PARENT DATA =====
    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone", nullable = false)
    private String parentPhone;

    @Column(name = "parent_email", nullable = false)
    private String parentEmail;

    @Column(name = "parent_address", columnDefinition = "TEXT", nullable = false)
    private String parentAddress;

    // ===== ADDRESS DATA =====
    @Column(name = "permanent_address", columnDefinition = "TEXT", nullable = false)
    private String permanentAddress;

    @Column(name = "current_address", columnDefinition = "TEXT")
    private String currentAddress;

    // ===== ALUMNI DATA =====
    @Column(name = "alumni_family")
    private Boolean alumniFamily = false;

    @Column(name = "alumni_name")
    private String alumniName;

    @Column(name = "alumni_relation")
    private String alumniRelation;

    // ===== DOCUMENT FILE PATHS (UNIFIED STORAGE) =====
    @Column(name = "pakta_integritas_file", columnDefinition = "TEXT")
    private String paktaIntegritasFile;

    @Column(name = "ijazah_file", columnDefinition = "TEXT")
    private String ijazahFile;

    @Column(name = "pasphoto_file", columnDefinition = "TEXT")
    private String pasphotoFile;

    @Column(name = "kartu_keluarga_file", columnDefinition = "TEXT")
    private String kartuKeluargaFile;

    @Column(name = "ktp_file", columnDefinition = "TEXT")
    private String ktpFile;

    @Column(name = "surat_bebas_narkoba_file", columnDefinition = "TEXT")
    private String suratBebasNarkobaFile;

    @Column(name = "skck_file", columnDefinition = "TEXT")
    private String skckFile;

    // ✅ OPTIONAL: Keep relationship for backward compatibility
    @OneToMany(mappedBy = "reenrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReEnrollmentDocument> documents = new ArrayList<>();

    // ===== STATUS & TRACKING =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReEnrollmentStatus status = ReEnrollmentStatus.INCOMPLETE;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

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

    public enum ReEnrollmentStatus {
        INCOMPLETE,    // Belum lengkap
        SUBMITTED,     // Sudah disubmit
        VALIDATED,     // Sudah divalidasi
        REJECTED,      // Ditolak
        COMPLETED      // Selesai
    }
}
