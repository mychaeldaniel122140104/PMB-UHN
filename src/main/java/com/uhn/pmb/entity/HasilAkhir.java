package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hasil_akhir", uniqueConstraints = {
        @UniqueConstraint(columnNames = "briva_number", name = "uk_briva"),
        @UniqueConstraint(columnNames = "nomor_registrasi", name = "uk_nomor_registrasi"),
        @UniqueConstraint(columnNames = "student_id", name = "uk_student_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HasilAkhir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== Student References =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ===== Final Results Data =====
    @Column(name = "briva_number", nullable = false, unique = true, length = 50)
    private String brivaNumber;

    @Column(name = "briva_amount", precision = 15, scale = 2)
    private BigDecimal brivaAmount;

    @Column(name = "jumlah_cicilan")
    private Integer jumlahCicilan; // Jumlah cicilan dari CICILAN_REQUEST

    @Column(name = "nomor_registrasi", nullable = false, unique = true, length = 100)
    private String nomorRegistrasi;

    // ===== Wave & Selection Info (NEW) =====
    @Enumerated(EnumType.STRING)
    @Column(name = "wave_type", length = 50)
    private RegistrationPeriod.WaveType waveType; // REGULAR_TEST, EARLY_NO_TEST, etc

    @Column(name = "selection_type", length = 100)
    private String selectionType; // KEDOKTERAN, NON_KEDOKTERAN, etc

    @Column(name = "program_studi_name", length = 255)
    private String programStudiName; // Program studi yang dipilih/diterima

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selection_period_id")
    private RegistrationPeriod selectionPeriod; // Reference to registration period

    // ===== Dokumen Sementara (NPM & KTM) =====
    @Column(name = "npm_sementara_file", length = 500)
    private String npmSementaraFile; // Path to uploaded NPM Sementara PDF

    @Column(name = "ktm_sementara_file", length = 500)
    private String ktmSementaraFile; // Path to uploaded KTM Sementara PDF

    // ===== Status =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HasilAkhirStatus status = HasilAkhirStatus.PENDING;

    // ===== Timestamps =====
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

    // ===== Enum untuk Status =====
    public enum HasilAkhirStatus {
        PENDING,      // Menunggu diaktifkan
        ACTIVE,       // Aktif dan dapat digunakan
        EXPIRED,      // Sudah expired
        USED,         // Sudah digunakan
        CANCELLED     // Dibatalkan
    }
}
