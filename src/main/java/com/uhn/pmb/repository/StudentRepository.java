package com.uhn.pmb.repository;

import com.uhn.pmb.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser_Id(Long userId);
    Optional<Student> findByNik(String nik);
    List<Student> findByFullNameContainingIgnoreCase(String fullName);
}
