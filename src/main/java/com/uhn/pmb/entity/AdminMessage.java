package com.uhn.pmb.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * Entity untuk CS messages antara student dan admin
 */
@Entity
@Table(name = "admin_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonIgnore
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    @JsonIgnore
    private User recipient;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageContent;

    @Column(name = "message_type")
    private String messageType; // QUESTION, ANSWER, INFO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.UNREAD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_message_id")
    @JsonIgnore
    private AdminMessage parentMessage; // Untuk reply thread

    @Column(name = "admission_form_id")
    private Long admissionFormId; // Reference jika ada konteks ke form tertentu

    public enum MessageStatus {
        UNREAD,    // Belum dibaca
        READ,      // Sudah dibaca
        REPLIED    // Sudah dibalas
    }
}
