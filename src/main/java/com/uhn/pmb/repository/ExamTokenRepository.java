package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ExamToken;
import com.uhn.pmb.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamTokenRepository extends JpaRepository<ExamToken, Long> {

    // Cari token by token value
    Optional<ExamToken> findByTokenValue(String tokenValue);

    // Cari token by student
    Optional<ExamToken> findByStudent(Student student);

    // Cari token by student ID
    Optional<ExamToken> findByStudentId(Long studentId);

    // Cari semua token untuk student tertentu
    List<ExamToken> findAllByStudentId(Long studentId);

    // Cari token by status
    List<ExamToken> findByStatus(ExamToken.TokenStatus status);

    // Cari token yang sudah expired
    List<ExamToken> findByExpiresAtBefore(LocalDateTime dateTime);

    // Cari token yang active (belum digunakan)
    List<ExamToken> findByStatusAndExpiresAtAfter(ExamToken.TokenStatus status, LocalDateTime dateTime);

    // Cari total token per status
    Long countByStatus(ExamToken.TokenStatus status);

    // Cari total token yang sudah digunakan (USED status)
    Long countByStatusAndUsedAtNotNull(ExamToken.TokenStatus status);
}
