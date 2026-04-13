package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private AdmissionForm admissionForm;

    @Column(name = "va_number", unique = true, nullable = false)
    private String vaNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VAStatus status = VAStatus.ACTIVE;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "briva_reference")
    private String brivaReference;

    @Column(columnDefinition = "TEXT")
    private String paymentInfo;

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

    public enum PaymentType {
        REGISTRATION_FORM,    // Biaya Formulir Pendaftaran
        INSTALLMENT_1,        // Cicilan 1
        INSTALLMENT_2,        // Cicilan 2
        INSTALLMENT_3         // Cicilan 3
    }

    public enum VAStatus {
        ACTIVE,               // Menunggu pembayaran
        PAID,                 // Sudah lunas
        EXPIRED,              // Kadaluarsa
        CANCELLED             // Dibatalkan
    }
}
