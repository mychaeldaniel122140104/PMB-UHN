package com.uhn.pmb.repository;

import com.uhn.pmb.entity.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    List<RegistrationPeriod> findByStatus(RegistrationPeriod.Status status);
    List<RegistrationPeriod> findByStatusOrderByRegStartDateDesc(RegistrationPeriod.Status status);
}
