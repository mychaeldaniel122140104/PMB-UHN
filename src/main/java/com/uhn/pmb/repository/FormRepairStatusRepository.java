package com.uhn.pmb.repository;

import com.uhn.pmb.entity.FormRepairStatus;
import com.uhn.pmb.entity.FormValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormRepairStatusRepository extends JpaRepository<FormRepairStatus, Long> {
    
    /**
     * Find repair status by form validation ID
     */
    Optional<FormRepairStatus> findByFormValidationId(Long formValidationId);
    
    /**
     * Find all repair statuses by form validation
     */
    List<FormRepairStatus> findByFormValidation(FormValidation formValidation);
    
    /**
     * Check if repair status exists for form validation
     */
    boolean existsByFormValidationId(Long formValidationId);
}
