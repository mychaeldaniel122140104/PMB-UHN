package com.uhn.pmb.repository;

import com.uhn.pmb.entity.PublicationSchedule;
import com.uhn.pmb.entity.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationScheduleRepository extends JpaRepository<PublicationSchedule, Long> {

    Optional<PublicationSchedule> findByPeriod(RegistrationPeriod period);

    Optional<PublicationSchedule> findByPeriodId(Long periodId);

    List<PublicationSchedule> findByIsPublishedFalseAndPublishDateTimeBefore(LocalDateTime now);

    List<PublicationSchedule> findAllByOrderByPublishDateTimeDesc();
}
