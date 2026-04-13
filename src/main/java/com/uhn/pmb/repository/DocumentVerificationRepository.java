package com.uhn.pmb.repository;

import com.uhn.pmb.entity.DocumentVerification;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVerificationRepository extends JpaRepository<DocumentVerification, Long> {
    List<DocumentVerification> findByUser(User user);
    List<DocumentVerification> findByStatus(DocumentVerification.VerificationStatus status);
    Optional<DocumentVerification> findByUserAndDocumentType(User user, DocumentVerification.DocumentType type);
}
