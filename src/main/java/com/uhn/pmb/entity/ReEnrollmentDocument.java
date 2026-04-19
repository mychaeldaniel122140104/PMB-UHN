package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reenrollment_documents")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReEnrollmentDocument {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reenrollment_id", nullable = false)
    private ReEnrollment reenrollment;

    // Document type
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    // Document file path
    @Column(name = "file_path", columnDefinition = "TEXT", nullable = false)
    private String filePath;

    // Original file name
    @Column(name = "original_filename")
    private String originalFilename;

    // File size in bytes
    @Column(name = "file_size")
    private Long fileSize;

    // File MIME type
    @Column(name = "file_mime_type")
    private String fileMimeType;

    // Upload status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    // Admin validation status for this document
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    // Admin notes for this specific document
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "validated_by_admin_id")
    private User validatedByAdmin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DocumentType {
        PAKTA_INTEGRITAS("Pakta Integritas"),
        IJAZAH("Ijazah"),
        PASPHOTO("Pasphoto"),
        KARTU_KELUARGA("Kartu Keluarga"),
        KARTU_TANDA_PENDUDUK("Kartu Tanda Penduduk"),
        KETERANGAN_BEBAS_NARKOBA("Keterangan Bebas Narkoba"),
        SKCK("SKCK");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum UploadStatus {
        PENDING,       // Menunggu upload
        COMPLETED,     // Upload selesai
        FAILED,        // Upload gagal
        INVALID        // File tidak valid
    }

    public enum ValidationStatus {
        PENDING,       // Belum divalidasi
        APPROVED,      // Disetujui
        REJECTED,      // Ditolak
        REVISION_NEEDED // Perlu revisi
    }
}
