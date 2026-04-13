package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ManualPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManualPaymentRepository extends JpaRepository<ManualPayment, Long> {
    
    Optional<ManualPayment> findByAdmissionFormId(Long admissionFormId);
    
    List<ManualPayment> findByStatus(ManualPayment.PaymentStatus status);
    
    Page<ManualPayment> findByStatus(ManualPayment.PaymentStatus status, Pageable pageable);
    
    List<ManualPayment> findByAdmissionFormIdIn(List<Long> admissionFormIds);
}
