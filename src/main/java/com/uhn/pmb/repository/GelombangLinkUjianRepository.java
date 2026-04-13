package com.uhn.pmb.repository;

import com.uhn.pmb.entity.GelombangLinkUjian;
import com.uhn.pmb.entity.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GelombangLinkUjianRepository extends JpaRepository<GelombangLinkUjian, Long> {
    
    /**
     * Find ujian link by registration period ID
     */
    Optional<GelombangLinkUjian> findByRegistrationPeriodId(Long periodId);
    
    /**
     * Find ujian link by registration period entity
     */
    Optional<GelombangLinkUjian> findByRegistrationPeriod(RegistrationPeriod period);
    
    /**
     * Get all ujian links (ordered by updated_at DESC)
     */
    List<GelombangLinkUjian> findAllByOrderByUpdatedAtDesc();
    
    /**
     * Delete by registration period ID
     */
    void deleteByRegistrationPeriodId(Long periodId);
}
