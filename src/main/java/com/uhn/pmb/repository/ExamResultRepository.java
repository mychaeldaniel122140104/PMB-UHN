package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findByExam_Id(Long examId);
    Optional<ExamResult> findByAdmissionNumber(String admissionNumber);
    Optional<ExamResult> findByStudent_Id(Long studentId);
    List<ExamResult> findByExamValidationStatus(ExamResult.ExamValidationStatus status);
}
