package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ReEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReEnrollmentRepository extends JpaRepository<ReEnrollment, Long> {
    Optional<ReEnrollment> findByStudent_Id(Long studentId);
    List<ReEnrollment> findByStatus(ReEnrollment.ReEnrollmentStatus status);
}
