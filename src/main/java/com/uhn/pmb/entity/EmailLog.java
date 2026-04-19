package com.uhn.pmb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String recipientEmail;
    
    @Column(nullable = false)
    private String subject;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType emailType;
    
    private LocalDateTime sentDate;
    
    private Boolean successStatus;
    
    private String errorMessage;
    
    private String attachmentUrl;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    
    @PrePersist
    protected void onCreate() {
        sentDate = LocalDateTime.now();
    }
    
    public enum EmailType {
        KARTU_UJIAN_NEW_REGISTRATION,
        KARTU_UJIAN_FROM_PAYMENT,
        NPM_ASSIGNMENT,
        CICILAN_PAYMENT_REMINDER,
        CICILAN_PAYMENT_OVERDUE,
        ADMIN_NOTIFICATION_NEW_DOCUMENT,
        VERIFICATION_COMPLETE,
        FORM_RECEIVED_CONFIRMATION,
        PAYMENT_CONFIRMATION
    }
}
