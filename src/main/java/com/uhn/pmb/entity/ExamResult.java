package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    // ✅ NEW: Reference to Student who took the exam
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private Double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultStatus status = ResultStatus.PENDING;

    @Column(name = "admission_number")
    private String admissionNumber;

    @Column(name = "admission_password", columnDefinition = "TEXT")
    private String admissionPassword;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ✅ NEW: Student exam submission fields
    @Column(name = "student_input_token", length = 255)
    private String studentInputToken;

    @Column(name = "generated_token", length = 255)
    private String generatedToken;

    @Column(name = "proof_photo_path", columnDefinition = "TEXT")
    private String proofPhotoPath;

    @Column(name = "gform_score")
    private Double gformScore;

    @Column(name = "token_validated")
    private Boolean tokenValidated = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_validation_status")
    private ExamValidationStatus examValidationStatus = ExamValidationStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "validated_by_admin_id")
    private User validatedByAdmin;

    @Column(name = "exam_validated_at")
    private LocalDateTime examValidatedAt;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

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

    public enum ResultStatus {
        PASSED,        // Lulus
        FAILED,        // Tidak lulus
        PENDING,       // Menunggu pengumuman
        PUBLISHED      // Sudah dipublikasikan
    }

    public enum ExamValidationStatus {
        PENDING,       // Belum divalidasi admin
        APPROVED,      // Disetujui admin
        REJECTED,      // Ditolak admin (token tidak cocok/fraud)
        REVISI         // Admin minta revisi (upload ulang bukti/token)
    }
}
