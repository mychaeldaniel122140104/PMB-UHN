package com.uhn.pmb.repository;

import com.uhn.pmb.entity.PeriodJenisSeleksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodJenisSeleksiRepository extends JpaRepository<PeriodJenisSeleksi, Long> {
    
    /**
     * Find all active jenis seleksi for a specific period
     */
    List<PeriodJenisSeleksi> findByPeriod_IdAndIsActiveTrue(Long periodId);
    
    /**
     * Find all jenis seleksi for a specific period (including inactive)
     */
    List<PeriodJenisSeleksi> findByPeriod_Id(Long periodId);
    
    /**
     * Find a specific relationship between a period and jenis seleksi
     */
    Optional<PeriodJenisSeleksi> findByPeriod_IdAndJenisSeleksi_Id(Long periodId, Long jenisSeleksiId);
    
    /**
     * Check if a jenis seleksi is used in any period
     */
    boolean existsByJenisSeleksi_Id(Long jenisSeleksiId);
    
    /**
     * Delete all relationships for a specific period
     */
    void deleteByPeriod_Id(Long periodId);
    
    /**
     * Delete all relationships for a specific jenis seleksi
     */
    void deleteByJenisSeleksi_Id(Long jenisSeleksiId);
}
