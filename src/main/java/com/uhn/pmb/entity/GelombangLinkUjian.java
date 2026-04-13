package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gelombang_link_ujian")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GelombangLinkUjian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "registration_period_id", nullable = false, unique = true)
    private RegistrationPeriod registrationPeriod;

    // Online Mode: Google Form Link
    @Column(name = "link_ujian", columnDefinition = "VARCHAR(500)")
    private String linkUjian;

    // Offline Mode: Exam Details
    @Column(name = "exam_date", columnDefinition = "VARCHAR(100)")
    private String examDate;

    @Column(name = "exam_place", columnDefinition = "VARCHAR(255)")
    private String examPlace;

    @Column(name = "exam_time", columnDefinition = "VARCHAR(100)")
    private String examTime;

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
