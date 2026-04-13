package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ManualCicilanPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManualCicilanPaymentRepository extends JpaRepository<ManualCicilanPayment, Long> {
    
    List<ManualCicilanPayment> findByCicilanRequestId(Long cicilanRequestId);

    List<ManualCicilanPayment> findByStudent_Id(Long studentId);
    
    @Query("SELECT m FROM ManualCicilanPayment m WHERE m.status = 'PENDING' ORDER BY m.createdAt ASC")
    Page<ManualCicilanPayment> findPendingPayments(Pageable pageable);
    
    @Query("SELECT m FROM ManualCicilanPayment m WHERE m.student.id = :studentId AND m.cicilanRequest.id = :cicilanRequestId ORDER BY m.cicilanKe ASC")
    List<ManualCicilanPayment> findByStudentAndCicilanRequest(Long studentId, Long cicilanRequestId);
    
    Optional<ManualCicilanPayment> findByCicilanRequestIdAndCicilanKe(Long cicilanRequestId, Integer cicilanKe);
}
