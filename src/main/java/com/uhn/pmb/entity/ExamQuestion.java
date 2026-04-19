package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * Entity untuk soal ujian yang di-generate oleh AI
 * Admin dapat approve/reject sebelum ditambahkan ke database
 */
@Entity
@Table(name = "exam_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionCategory category; // IPA, IPS, Psikotes, Bahasa

    @Column(nullable = false)
    private String subject; // Biologi, Kimia, Fisika, Matematika, Sejarah, dll

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionDifficulty difficulty; // Easy, Medium, Hard

    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String optionA;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String optionB;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String optionC;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String optionD;

    @Column(columnDefinition = "TEXT")
    private String optionE;

    @Column(nullable = false)
    private String correctAnswer; // A, B, C, D, E

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id", nullable = false)
    @JsonIgnore
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by_id")
    @JsonIgnore
    private User approvedBy;
}
