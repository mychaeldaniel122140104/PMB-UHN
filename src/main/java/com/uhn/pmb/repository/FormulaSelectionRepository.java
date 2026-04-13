package com.uhn.pmb.repository;

import com.uhn.pmb.entity.FormulaSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaSelectionRepository extends JpaRepository<FormulaSelection, Long> {
    Optional<FormulaSelection> findByCode(String code);
    List<FormulaSelection> findByIsActiveTrueOrderBySortOrder();
    List<FormulaSelection> findAllByOrderBySortOrder();
}
