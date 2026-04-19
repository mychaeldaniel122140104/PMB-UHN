package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ProgramStudi represents academic study programs (e.g., Teknik Informatika, Sistem Informasi)
 * This is master data - can be reused across different selection types
 */
@Entity
@Table(name = "program_studi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramStudi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kode; // e.g., "TI", "SI", "KES"

    @Column(nullable = false)
    private String nama; // e.g., "Teknik Informatika", "Sistem Informasi"

    @Column(length = 100)
    private String fakultas; // e.g., "Teknik", "FKIP", "Ekonomi & Bisnis"

    @Column(columnDefinition = "TEXT")
    private String deskripsi; // Detailed description

    @Column(name = "is_medical")
    private Boolean isMedical = false; // true for medical programs like Kedokteran, Keperawatan

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer sortOrder = 0; // For ordering in UI

    @Column(name = "harga_total_per_tahun")
    private Long hargaTotalPerTahun = 0L; // Total program fee per year (can be divided into installments)

    @Column(name = "cicilan_1")
    private Long cicilan1 = 0L; // First installment amount

    @Column(name = "cicilan_2")
    private Long cicilan2 = 0L;

    @Column(name = "cicilan_3")
    private Long cicilan3 = 0L;

    @Column(name = "cicilan_4")
    private Long cicilan4 = 0L;

    @Column(name = "cicilan_5")
    private Long cicilan5 = 0L;

    @Column(name = "cicilan_6")
    private Long cicilan6 = 0L;

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
}
