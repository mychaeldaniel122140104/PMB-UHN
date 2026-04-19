package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "token_value", unique = true, nullable = false, length = 50)
    private String tokenValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "approved_form_id")
    private Long approvedFormId;

    // ===== HELPER METHODS =====
    public boolean isActive() {
        return status == TokenStatus.ACTIVE && 
               LocalDateTime.now().isBefore(expiresAt) &&
               usedAt == null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            // Default: 2 hours expiration
            expiresAt = LocalDateTime.now().plusHours(2);
        }
        if (tokenValue == null) {
            tokenValue = generateTokenValue();
        }
        if (status == null) {
            status = TokenStatus.ACTIVE;
        }
    }

    /**
     * Generate token format: UHN-TOKEN-XXXXX (seperti: UHN-TOKEN-A1B2C3)
     */
    private String generateTokenValue() {
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();
        return "UHN-TOKEN-" + randomPart;
    }

    public enum TokenStatus {
        ACTIVE,      // Token valid dan bisa digunakan
        USED,        // Token sudah digunakan untuk ujian
        EXPIRED,     // Token sudah expired
        REVOKED      // Token dibatalkan oleh admin
    }
}
