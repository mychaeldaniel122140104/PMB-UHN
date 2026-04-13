package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ValidationStatusTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ValidationStatusTrackerRepository extends JpaRepository<ValidationStatusTracker, Long> {

    Optional<ValidationStatusTracker> findByAdmissionFormId(Long admissionFormId);

    Optional<ValidationStatusTracker> findByStudentId(Long studentId);
}
