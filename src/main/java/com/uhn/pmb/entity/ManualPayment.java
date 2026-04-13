package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manual_payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_form_id", nullable = false)
    private AdmissionForm admissionForm;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.MANUAL;
    
    @Column(name = "amount", nullable = false)
    private Long amount; // Dalam Rupiah
    
    @Column(name = "proof_image_path", length = 500)
    private String proofImagePath; // Path ke file bukti transfer
    
    @Column(name = "bank_name", length = 100)
    private String bankName; // Nama bank pengirim
    
    @Column(name = "account_holder", length = 200)
    private String accountHolder; // Nama rekening pengirim
    
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate; // Tanggal transfer actual
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason; // Alasan penolakan jika REJECTED
    
    @Column(name = "verified_by", length = 200)
    private String verifiedBy; // Email admin yang verifikasi
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt; // Waktu verifikasi
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Enums
    public enum PaymentMethod {
        MANUAL("Manual Transfer")
        ;
        
        private final String label;
        PaymentMethod(String label) {
            this.label = label;
        }
        public String getLabel() { return label; }
    }
    
    public enum PaymentStatus {
        PENDING("Menunggu Verifikasi"),
        VERIFIED("Terverifikasi"),
        REJECTED("Ditolak")
        ;
        
        private final String label;
        PaymentStatus(String label) {
            this.label = label;
        }
        public String getLabel() { return label; }
    }
}
