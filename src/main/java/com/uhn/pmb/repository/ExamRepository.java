package com.uhn.pmb.repository;

import com.uhn.pmb.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    Optional<Exam> findByExamNumber(String examNumber);
    Optional<Exam> findByStudent_Id(Long studentId);
    List<Exam> findByPeriod_Id(Long periodId);
    List<Exam> findByStatus(Exam.ExamStatus status);
}
