package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "registration_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Gelombang 1"

    @Column(name = "reg_start_date", nullable = false)
    private LocalDateTime regStartDate;

    @Column(name = "reg_end_date", nullable = false)
    private LocalDateTime regEndDate;

    @Column(name = "exam_date", nullable = false)
    private LocalDateTime examDate;

    @Column(name = "exam_end_date", nullable = false)
    private LocalDateTime examEndDate;

    @Column(name = "announcement_date", nullable = false)
    private LocalDateTime announcementDate;

    @Column(name = "reenrollment_start_date", nullable = false)
    private LocalDateTime reenrollmentStartDate;

    @Column(name = "reenrollment_end_date", nullable = false)
    private LocalDateTime reenrollmentEndDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements; // e.g., "Wajib upload nilai, bukti ranking, dll"

    // ✅ NEW: Wave type - determines registration flow (EARLY_NO_TEST, RANKING_NO_TEST, REGULAR_TEST)
    @Enumerated(EnumType.STRING)
    @Column(name = "wave_type", nullable = false)
    private WaveType waveType = WaveType.REGULAR_TEST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ Many-to-Many relationship with Jenis Seleksi through junction table
    @OneToMany(mappedBy = "period", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<PeriodJenisSeleksi> periodJenisSeleksiList = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        OPEN, CLOSED, ARCHIVED
    }

    // ✅ NEW: Wave Type - determines registration flow logic
    public enum WaveType {
        EARLY_NO_TEST,      // Bebas tes sebelum UTBK, langsung lulus
        RANKING_NO_TEST,    // Bebas tes jika syarat ranking terpenuhi
        REGULAR_TEST        // Jalur normal dengan ujian
    }
}
