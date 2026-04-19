package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * Entity untuk tracking validasi formulir & pembayaran oleh admin
 */
@Entity
@Table(name = "form_validations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admission_form_id", nullable = false)
    @JsonIgnore
    private AdmissionForm admissionForm;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    private User validatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String rejectionTopic;

    @Column(name = "revision_number")
    private Integer revisionNumber = 1;  // Track which revision this is (1st, 2nd, 3rd, etc)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "virtual_account_number")
    private String virtualAccountNumber;

    @Column(name = "payment_amount")
    private Long paymentAmount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "exam_token", length = 255)
    private String examToken;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ValidationStatus {
        PENDING,              // Belum divalidasi
        APPROVED,             // Disetujui
        REJECTED,             // Ditolak
        REVISION_NEEDED,      // ✅ Perlu revisi/perbaikan data
        SUSPENDED             // Ditangguhkan
    }

    public enum PaymentStatus {
        PENDING,      // Belum membayar
        PAID,         // Sudah membayar
        VERIFIED      // Pembayaran terverifikasi
    }
}
