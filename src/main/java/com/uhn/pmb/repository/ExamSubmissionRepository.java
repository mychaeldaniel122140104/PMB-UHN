package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ExamSubmission;
import com.uhn.pmb.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {

    // Cari submission by student
    Optional<ExamSubmission> findByStudent(Student student);

    // Cari submission by student ID
    Optional<ExamSubmission> findByStudentId(Long studentId);

    // Cari submission by token
    Optional<ExamSubmission> findByExamTokenTokenValue(String tokenValue);

    // Cari semua submission untuk student
    List<ExamSubmission> findAllByStudentId(Long studentId);

    // Cari submission by status
    List<ExamSubmission> findByStatus(ExamSubmission.SubmissionStatus status);

    // Cari submission yang belum sync score
    List<ExamSubmission> findByStatusAndScoreSyncedAtNull(ExamSubmission.SubmissionStatus status);

    // Cari submission berdasarkan rentang waktu
    List<ExamSubmission> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Hitung total submission per status
    Long countByStatus(ExamSubmission.SubmissionStatus status);

    // Hitung total submission yang sudah completed
    Long countByStatusAndScoreSyncedAtNotNull(ExamSubmission.SubmissionStatus status);
}
