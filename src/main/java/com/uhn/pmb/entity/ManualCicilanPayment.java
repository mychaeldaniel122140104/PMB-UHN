package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manual_cicilan_payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualCicilanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== RELATIONSHIPS =====
    @ManyToOne
    @JoinColumn(name = "cicilan_request_id", nullable = false)
    private CicilanRequest cicilanRequest;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // ===== PAYMENT INFO =====
    @Column(name = "cicilan_ke", nullable = false)
    private Integer cicilanKe; // Which installment (1-6)

    @Column(name = "nominal", nullable = false)
    private Long nominal; // Amount to pay

    @Column(name = "payment_proof_path")
    private String paymentProofPath; // Path to uploaded proof image

    // ===== STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ManualPaymentStatus status = ManualPaymentStatus.PENDING;

    @Column(name = "keterangan", columnDefinition = "TEXT")
    private String keterangan; // Notes (rejection reason or approval notes)

    // ===== ADMIN VERIFICATION =====
    @Column(name = "verified_by")
    private String verifiedBy; // Admin username

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ===== TIMESTAMPS =====
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== ENUM =====
    public enum ManualPaymentStatus {
        PENDING("Menunggu Verifikasi"),
        VERIFIED("Terverifikasi"),
        REJECTED("Ditolak");

        private final String label;

        ManualPaymentStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
