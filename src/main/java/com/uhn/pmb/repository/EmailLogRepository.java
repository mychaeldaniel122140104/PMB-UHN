package com.uhn.pmb.repository;

import com.uhn.pmb.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientEmail(String email);
    List<EmailLog> findByEmailType(EmailLog.EmailType emailType);
    List<EmailLog> findBySuccessStatusAndSentDateBetween(Boolean success, LocalDateTime start, LocalDateTime end);
}
