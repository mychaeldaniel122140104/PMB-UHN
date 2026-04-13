package com.uhn.pmb.repository;

import com.uhn.pmb.entity.SelectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SelectionTypeRepository extends JpaRepository<SelectionType, Long> {
    List<SelectionType> findByPeriod_Id(Long periodId);
    List<SelectionType> findByPeriod_IdAndIsActiveTrue(Long periodId);
}
