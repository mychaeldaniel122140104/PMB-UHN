package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "university_bank_account")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityBankAccount {
    
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bank_name", length = 100, nullable = false)
    private String bankName; // Nama bank (BCA, Mandiri, BNI, dll)
    
    @Column(name = "account_number", length = 50, nullable = false)
    private String accountNumber; // Nomor rekening
    
    @Column(name = "account_holder", length = 200, nullable = false)
    private String accountHolder; // Nama pemilik rekening
    
    @Column(name = "purpose", length = 200, nullable = false)
    private String purpose; // Tujuan (Pendaftaran, Cicilan 1, Cicilan 2, dll)
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Status aktif/nonaktif
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
