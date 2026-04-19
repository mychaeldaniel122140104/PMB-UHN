package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_briva")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentBriva {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(unique = true, nullable = false)
    private String brivaCode;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPurpose purpose;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    private LocalDateTime dueDatetime;
    
    private LocalDateTime paidDatetime;
    
    private LocalDateTime createdAt;
    
    private String brivaReference;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
    
    public enum PaymentPurpose {
        FORMULIR_REGISTRATION,
        DAFTAR_ULANG_CICILAN_1,
        DAFTAR_ULANG_CICILAN_2,
        DAFTAR_ULANG_CICILAN_3,
        DAFTAR_ULANG_CICILAN_4
    }
    
    public enum PaymentStatus {
        PENDING,
        PAID,
        EXPIRED,
        FAILED
    }
    
    public boolean isOverdue() {
        if (status == PaymentStatus.PAID) return false;
        return LocalDateTime.now().isAfter(dueDatetime);
    }
}
