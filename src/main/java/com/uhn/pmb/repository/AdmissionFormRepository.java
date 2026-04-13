package com.uhn.pmb.repository;

import com.uhn.pmb.entity.AdmissionForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionFormRepository extends JpaRepository<AdmissionForm, Long> {
    Optional<AdmissionForm> findByStudent_IdAndPeriod_Id(Long studentId, Long periodId);
    List<AdmissionForm> findByStudent_Id(Long studentId);
    List<AdmissionForm> findByPeriod_Id(Long periodId);
    List<AdmissionForm> findByStatus(AdmissionForm.FormStatus status);
}
