package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ReEnrollmentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReEnrollmentDocumentRepository extends JpaRepository<ReEnrollmentDocument, Long> {
    
    // Find all documents for a specific re-enrollment
    List<ReEnrollmentDocument> findByReenrollmentId(Long reenrollmentId);
    
    // Find document by type
    Optional<ReEnrollmentDocument> findByReenrollmentIdAndDocumentType(
        Long reenrollmentId, 
        ReEnrollmentDocument.DocumentType documentType
    );
    
    // Find all documents of a specific type
    List<ReEnrollmentDocument> findByDocumentType(ReEnrollmentDocument.DocumentType documentType);
    
    // Find documents needing validation
    List<ReEnrollmentDocument> findByValidationStatus(ReEnrollmentDocument.ValidationStatus status);
    
    // Find documents by upload status
    List<ReEnrollmentDocument> findByUploadStatus(ReEnrollmentDocument.UploadStatus status);
    
    // Count documents for a re-enrollment
    int countByReenrollmentId(Long reenrollmentId);
    
    // Count approved documents for a re-enrollment
    int countByReenrollmentIdAndValidationStatus(
        Long reenrollmentId, 
        ReEnrollmentDocument.ValidationStatus status
    );
}
