package com.uhn.pmb.repository;

import com.uhn.pmb.entity.StudentNPM;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentNPMRepository extends JpaRepository<StudentNPM, Long> {
    Optional<StudentNPM> findByUser(User user);
    Optional<StudentNPM> findByNpm(String npm);
}
