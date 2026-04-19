package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_verification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;
    
    @Column(nullable = false)
    private String fileUrl;
    
    private LocalDateTime uploadDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;
    
    private String rejectionReason;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "verified_by_admin_id")
    private User verifiedByAdmin;
    
    private LocalDateTime verifiedDate;
    
    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
        if (status == null) {
            status = VerificationStatus.PENDING;
        }
    }
    
    public enum DocumentType {
        KARTU_KELUARGA,
        KTP,
        IJAZAH_SMA,
        NILAI_UTBK,
        SURAT_SEHAT,
        SURAT_TIDAK_BERHENTI_SEKOLAH
    }
    
    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
