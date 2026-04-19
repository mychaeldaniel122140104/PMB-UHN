package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "registration_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private RegistrationStage stage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RegistrationStatus_Enum status;
    
    @Column(name = "submission_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submissionDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Column(name = "edit_deadline")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editDeadline;
    
    @Column(name = "can_edit")
    private Boolean canEdit = true;
    
    @Column(name = "admin_verified")
    private Boolean adminVerified = false;
    
    @Column(name = "verified_by")
    private String verifiedBy;
    
    @Column(name = "verification_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verificationDate;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;
    
    @Column(name = "edit_count")
    private Integer editCount = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (editDeadline == null && submissionDate != null) {
            editDeadline = submissionDate.plusHours(24);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum RegistrationStage {
        GELOMBANG_SELECTION,
        FORMULA_SELECTION,
        PAYMENT_BRIVA,
        FORM_SUBMISSION,
        PSYCHO_EXAM,
        PAYMENT_CICILAN_1,
        DAFTAR_ULANG,
        DOCUMENT_VERIFICATION,
        COMPLETED
    }
    
    public enum RegistrationStatus_Enum {
        MENUNGGU_VERIFIKASI,
        SELESAI,
        REJECTED
    }
    
    // ===== Helper Methods =====
    
    public boolean isEditableByUser() {
        if (adminVerified) return false;
        if (editDeadline == null) return false;
        return LocalDateTime.now().isBefore(editDeadline);
    }
    
    public long getEditTimeRemainingHours() {
        if (!isEditableByUser()) return 0;
        return java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), editDeadline);
    }
}

