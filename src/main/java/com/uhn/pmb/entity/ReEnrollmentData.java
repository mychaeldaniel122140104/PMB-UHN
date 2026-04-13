package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reenrollment_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReEnrollmentData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private SelectionType program;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CicilanType cicilationType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")
    private User approvedByAdmin;
    
    private LocalDateTime approvalDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReEnrollmentStatus status;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReEnrollmentStatus.PENDING;
        }
    }
    
    public enum CicilanType {
        FULL_PAYMENT,
        CICILAN_2,
        CICILAN_3,
        CICILAN_4
    }
    
    public enum ReEnrollmentStatus {
        PENDING,
        ASSIGNED,
        PARTIAL_PAID,
        FULLY_PAID,
        VERIFIED,
        REJECTED
    }
    
    public int getTotalCicilan() {
        return switch (cicilationType) {
            case FULL_PAYMENT -> 1;
            case CICILAN_2 -> 2;
            case CICILAN_3 -> 3;
            case CICILAN_4 -> 4;
        };
    }
}
