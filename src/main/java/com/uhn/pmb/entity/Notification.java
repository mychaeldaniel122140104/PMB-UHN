package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        REGISTRATION_CONFIRMATION,    // Konfirmasi registrasi
        VA_GENERATED,                 // VA sudah dibuat
        PAYMENT_CONFIRMED,            // Pembayaran dikonfirmasi
        EXAM_READY,                   // Siap ujian
        EXAM_REMINDER,                // Reminder ujian
        RESULT_PUBLISHED,             // Hasil ujian dipublikasikan
        REENROLLMENT_REMINDER,        // Reminder daftar ulang
        VALIDATION_REJECTED,          // Data ditolak validasi
        VALIDATION_APPROVED,          // Data disetujui validasi
        SYSTEM_MESSAGE                // Pesan sistem umum
    }

    public enum NotificationStatus {
        PENDING,   // Belum dikirim
        SENT,      // Sudah dikirim
        FAILED,    // Gagal dikirim
        DELIVERED  // Terkirim (untuk push notification)
    }
}
