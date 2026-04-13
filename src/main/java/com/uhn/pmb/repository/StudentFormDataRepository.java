package com.uhn.pmb.repository;

import com.uhn.pmb.entity.StudentFormData;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFormDataRepository extends JpaRepository<StudentFormData, Long> {
    List<StudentFormData> findByUser(User user);
    Optional<StudentFormData> findByUserAndFormVersion(User user, Integer version);
}
