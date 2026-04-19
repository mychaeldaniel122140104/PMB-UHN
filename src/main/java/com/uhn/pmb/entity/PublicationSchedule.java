package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "publication_schedule", uniqueConstraints = {
        @UniqueConstraint(columnNames = "period_id", name = "uk_pub_schedule_period")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "period_id", nullable = false, unique = true)
    private RegistrationPeriod period;

    @Column(name = "publish_date_time", nullable = false)
    private LocalDateTime publishDateTime;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

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

    /**
     * Check if results should be visible now based on schedule
     */
    public boolean isResultsVisible() {
        if (isPublished != null && isPublished) {
            return true;
        }
        return publishDateTime != null && LocalDateTime.now().isAfter(publishDateTime);
    }
}
