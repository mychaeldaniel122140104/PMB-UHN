package com.uhn.pmb.repository;

import com.uhn.pmb.entity.HasilAkhir;
import com.uhn.pmb.entity.Student;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HasilAkhirRepository extends JpaRepository<HasilAkhir, Long> {

    // Find by student entity
    Optional<HasilAkhir> findByStudent(Student student);

    // Find by user entity
    Optional<HasilAkhir> findByUser(User user);

    // Find by BRIVA number
    Optional<HasilAkhir> findByBrivaNumber(String brivaNumber);

    // Find by nomor registrasi
    Optional<HasilAkhir> findByNomorRegistrasi(String nomorRegistrasi);

    // Find all by status
    List<HasilAkhir> findByStatus(HasilAkhir.HasilAkhirStatus status);

    // Find all active results
    List<HasilAkhir> findByStatusIn(List<HasilAkhir.HasilAkhirStatus> statuses);

    // Check if student has hasil akhir
    boolean existsByStudent(Student student);
}
