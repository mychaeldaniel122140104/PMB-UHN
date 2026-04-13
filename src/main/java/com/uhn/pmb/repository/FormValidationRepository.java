package com.uhn.pmb.repository;

import com.uhn.pmb.entity.FormValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormValidationRepository extends JpaRepository<FormValidation, Long> {

    Optional<FormValidation> findByAdmissionFormId(Long admissionFormId);

    List<FormValidation> findByValidationStatus(FormValidation.ValidationStatus status);

    List<FormValidation> findByValidationStatusOrderByCreatedAtDesc(FormValidation.ValidationStatus status);

    @Query("SELECT fv FROM FormValidation fv WHERE fv.validationStatus = :status ORDER BY fv.createdAt DESC")
    List<FormValidation> findAllByStatusOrdered(@Param("status") FormValidation.ValidationStatus status);

    @Query("SELECT fv FROM FormValidation fv WHERE fv.student.id = :studentId")
    Optional<FormValidation> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(fv) FROM FormValidation fv WHERE fv.validationStatus = 'PENDING'")
    Long countPendingValidations();

    @Query("SELECT fv FROM FormValidation fv WHERE fv.paymentStatus = :paymentStatus")
    List<FormValidation> findByPaymentStatus(@Param("paymentStatus") FormValidation.PaymentStatus paymentStatus);
}
