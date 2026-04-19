package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Junction entity for many-to-many relationship between JenisSeleksi and ProgramStudi
 * This allows each selection type to have multiple study programs
 */
@Entity
@Table(name = "selection_program_studi", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"jenis_seleksi_id", "program_studi_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectionProgramStudi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "jenis_seleksi_id", nullable = false)
    private JenisSeleksi jenisSeleksi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "program_studi_id", nullable = false)
    private ProgramStudi programStudi;

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
