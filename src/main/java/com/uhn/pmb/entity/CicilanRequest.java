package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cicilan_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CicilanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== RELATIONSHIPS =====
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "program_studi_id", nullable = false)
    private ProgramStudi programStudi;

    @ManyToOne
    @JoinColumn(name = "admission_form_id", nullable = false)
    private AdmissionForm admissionForm;

    // ===== CICILAN INFO =====
    @Column(name = "jumlah_cicilan", nullable = false)
    private Integer jumlahCicilan; // 1-6 kali

    @Column(name = "harga_cicilan_1", nullable = false)
    private Long hargaCicilan1; // Harga cicilan pertama (dari ProgramStudi)

    @Column(name = "harga_cicilan_2")
    private Long hargaCicilan2 = 0L;

    @Column(name = "harga_cicilan_3")
    private Long hargaCicilan3 = 0L;

    @Column(name = "harga_cicilan_4")
    private Long hargaCicilan4 = 0L;

    @Column(name = "harga_cicilan_5")
    private Long hargaCicilan5 = 0L;

    @Column(name = "harga_cicilan_6")
    private Long hargaCicilan6 = 0L;

    @Column(name = "harga_total", nullable = false)
    private Long hargaTotal; // Total harga (dari ProgramStudi)

    @Column(name = "harga_per_cicilan")
    private Long hargaPerCicilan; // hargaTotal / jumlahCicilan (legacy, kept for compat)

    // ===== PAYMENT INFO =====
    @Column(name = "briva", length = 50)
    private String briva; // Virtual Account BRIVA number (added by admin)

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.SIMULATION;

    // ===== STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CicilanRequestStatus status = CicilanRequestStatus.PENDING;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan; // Untuk reject reason atau note

    // ===== ADMIN APPROVAL =====
    @Column(name = "approved_by")
    private String approvedBy; // Admin username

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ===== TIMESTAMPS =====
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== ENUM =====
    public enum CicilanRequestStatus {
        PENDING("Menunggu Validasi"),
        APPROVED("Disetujui"),
        REJECTED("Ditolak");

        private final String label;

        CicilanRequestStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum PaymentMethod {
        SIMULATION("Virtual Account (Simulasi)"),
        MANUAL("Transfer Bank Manual");

        private final String label;

        PaymentMethod(String label) {
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
