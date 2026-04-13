package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ExamLink;
import com.uhn.pmb.entity.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamLinkRepository extends JpaRepository<ExamLink, Long> {
    List<ExamLink> findByPeriodId(Long periodId);
    List<ExamLink> findByPeriod(RegistrationPeriod period);
    List<ExamLink> findByPeriodAndIsActiveTrue(RegistrationPeriod period);
}
