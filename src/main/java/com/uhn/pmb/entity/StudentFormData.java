package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_form_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFormData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gelombang_id")
    private RegistrationPeriod gelombang;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formula_id")
    private SelectionType formula;
    
    private Integer formVersion;
    
    @Column(columnDefinition = "TEXT")
    private String personalDataJson;
    
    @Column(columnDefinition = "TEXT")
    private String parentDataJson;
    
    @Column(columnDefinition = "TEXT")
    private String educationDataJson;
    
    private String examNumber;
    
    private LocalDateTime submittedAt;
    
    private LocalDateTime modifiedAt;
    
    @PrePersist
    protected void onCreate() {
        modifiedAt = LocalDateTime.now();
        if (formVersion == null) {
            formVersion = 1;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
