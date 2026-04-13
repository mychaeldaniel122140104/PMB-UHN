package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(length = 100, nullable = false, unique = true)
    private String linkName;
    
    @Column(length = 50, nullable = false)
    private String linkType; // YOUTUBE, GOOGLE_FORM, GOOGLE_DRIVE, EXTERNAL_URL
    
    @Column(length = 500, nullable = false)
    private String linkUrl;
    
    @Column(length = 255)
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
