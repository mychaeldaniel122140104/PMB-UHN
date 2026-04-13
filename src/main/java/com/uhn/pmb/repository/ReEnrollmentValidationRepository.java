package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ReEnrollmentValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReEnrollmentValidationRepository extends JpaRepository<ReEnrollmentValidation, Long> {

    Optional<ReEnrollmentValidation> findByReEnrollmentId(Long reEnrollmentId);

    List<ReEnrollmentValidation> findByValidationStatus(ReEnrollmentValidation.ValidationStatus status);

    List<ReEnrollmentValidation> findByValidationStatusOrderByCreatedAtDesc(ReEnrollmentValidation.ValidationStatus status);

    @Query("SELECT rv FROM ReEnrollmentValidation rv WHERE rv.validationStatus = :status ORDER BY rv.createdAt DESC")
    List<ReEnrollmentValidation> findAllByStatusOrdered(@Param("status") ReEnrollmentValidation.ValidationStatus status);

    @Query("SELECT rv FROM ReEnrollmentValidation rv WHERE rv.student.id = :studentId")
    Optional<ReEnrollmentValidation> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(rv) FROM ReEnrollmentValidation rv WHERE rv.validationStatus = 'PENDING'")
    Long countPendingValidations();
}
