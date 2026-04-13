package com.uhn.pmb.repository;

import com.uhn.pmb.entity.SelectionProgramStudi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SelectionProgramStudiRepository extends JpaRepository<SelectionProgramStudi, Long> {
    
    /**
     * Find all active program studi for a specific jenis seleksi
     */
    List<SelectionProgramStudi> findByJenisSeleksi_IdAndIsActiveTrue(Long jenisSeleksiId);
    
    /**
     * Find all program studi for a specific jenis seleksi (including inactive)
     */
    List<SelectionProgramStudi> findByJenisSeleksi_Id(Long jenisSeleksiId);
    
    /**
     * Find a specific relationship between jenis seleksi and program studi
     */
    Optional<SelectionProgramStudi> findByJenisSeleksi_IdAndProgramStudi_Id(Long jenisSeleksiId, Long programStudiId);
    
    /**
     * Check if a program studi is used in any jenis seleksi
     */
    boolean existsByProgramStudi_Id(Long programStudiId);
    
    /**
     * Delete all program studi relationships for a jenis seleksi
     */
    void deleteByJenisSeleksi_Id(Long jenisSeleksiId);
}
