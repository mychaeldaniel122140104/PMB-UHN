package com.uhn.pmb.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity untuk tracking repair status dari form validation
 * Terpisah dari FormValidation untuk better design
 */
@Entity
@Table(name = "form_repair_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormRepairStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "form_validation_id", nullable = false)
    private FormValidation formValidation;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus status = RepairStatus.BELUM_PERBAIKAN;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum RepairStatus {
        BELUM_PERBAIKAN("Belum Diperbaiki"),
        SUDAH_PERBAIKAN("Sudah Diperbaiki");
        
        private final String displayName;
        
        RepairStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
