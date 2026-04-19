package com.uhn.pmb.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JenisSeleksi represents different selection methods available to students
 * (e.g., "Regular", "Scholarship", "Excellence")
 * Independent from periods and waves - can be reused across periods
 */
@Entity
@Table(name = "jenis_seleksi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JenisSeleksi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g., "REGULAR", "SCHOLARSHIP", "EXCELLENCE"

    @Column(nullable = false)
    private String nama; // e.g., "Seleksi Reguler", "Seleksi Beasiswa"

    @Column(columnDefinition = "TEXT")
    private String deskripsi; // Detailed description

    @Column(columnDefinition = "TEXT")
    private String fasilitas; // JSON or comma-separated features

    @Column(name = "logo_url")
    private String logoUrl; // URL or emoji for logo

    @Column(nullable = false)
    private BigDecimal harga; // Registration fee

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer sortOrder = 0; // For ordering in UI

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "jenisSeleksi", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<SelectionProgramStudi> selectionProgramStudiList = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "jenisSeleksi", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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
}
