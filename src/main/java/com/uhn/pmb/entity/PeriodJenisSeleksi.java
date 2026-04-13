package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Junction entity for many-to-many relationship between RegistrationPeriod (Gelombang) and JenisSeleksi
 * This allows each period to have multiple selection types
 */
@Entity
@Table(name = "period_jenis_seleksi", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"period_id", "jenis_seleksi_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodJenisSeleksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jenis_seleksi_id", nullable = false)
    private JenisSeleksi jenisSeleksi;

    @Column(nullable = false)
    private Boolean isActive = true;

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
