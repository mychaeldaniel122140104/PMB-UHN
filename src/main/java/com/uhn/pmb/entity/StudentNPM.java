package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_npm")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentNPM {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(unique = true, nullable = false)
    private String npm;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "program_id", nullable = false)
    private SelectionType program;
    
    private LocalDateTime programmingStartDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;
    
    private LocalDateTime issuedDate;
    
    @PrePersist
    protected void onCreate() {
        issuedDate = LocalDateTime.now();
        if (status == null) {
            status = StudentStatus.ACTIVE;
        }
    }
    
    public enum StudentStatus {
        ACTIVE,
        INACTIVE
    }
}
