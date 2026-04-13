package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ReEnrollmentData;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReEnrollmentDataRepository extends JpaRepository<ReEnrollmentData, Long> {
    Optional<ReEnrollmentData> findByUser(User user);
    List<ReEnrollmentData> findByStatus(ReEnrollmentData.ReEnrollmentStatus status);
}
