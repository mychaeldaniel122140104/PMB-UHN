package com.uhn.pmb.repository;

import com.uhn.pmb.entity.ProgramStudi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramStudiRepository extends JpaRepository<ProgramStudi, Long> {
    
    /**
     * Find program studi by kode
     */
    Optional<ProgramStudi> findByKode(String kode);
    
    /**
     * Find all active program studi ordered by sort order
     */
    List<ProgramStudi> findByIsActiveTrueOrderBySortOrder();
    
    /**
     * Find all program studi (including inactive) ordered by sort order
     */
    List<ProgramStudi> findAllByOrderBySortOrder();
    
    /**
     * Find program studi by medical category
     */
    List<ProgramStudi> findByIsMedicalAndIsActiveTrueOrderBySortOrder(Boolean isMedical);
    
    /**
     * Check if kode already exists
     */
    boolean existsByKode(String kode);
}
