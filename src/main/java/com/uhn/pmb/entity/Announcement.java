package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity untuk pengumuman sistem yang ditampilkan ke semua camaba
 */
@Entity
@Table(name = "announcements", indexes = {
    @Index(name = "idx_announcement_created", columnList = "created_at DESC"),
    @Index(name = "idx_announcement_active", columnList = "is_active DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String description; // Short description for listing

    @Column(name = "created_by_name", nullable = false)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0; // 0 = normal, 1 = high, 2 = urgent

    @Enumerated(EnumType.STRING)
    @Column(name = "announcement_type")
    @Builder.Default
    private AnnouncementType announcementType = AnnouncementType.GENERAL;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        publishedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AnnouncementType {
        GENERAL,      // Pengumuman umum
        UPCOMING,     // Pengumuman yang akan datang
        DEADLINE,     // Pengumuman deadline
        MAINTENANCE,  // Maintenance sistem
        IMPORTANT,    // Pengumuman penting
        EVENT         // Event atau acara
    }
}

