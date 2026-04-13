package com.uhn.pmb.repository;

import com.uhn.pmb.entity.RegistrationStatus;
import com.uhn.pmb.entity.RegistrationStatus.RegistrationStage;
import com.uhn.pmb.entity.RegistrationStatus.RegistrationStatus_Enum;
import com.uhn.pmb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationStatusRepository extends JpaRepository<RegistrationStatus, Long> {
    
    List<RegistrationStatus> findByUser(User user);
    
    List<RegistrationStatus> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<RegistrationStatus> findByUserAndStage(User user, RegistrationStage stage);
    
    List<RegistrationStatus> findByStageAndStatus(RegistrationStage stage, RegistrationStatus_Enum status);
    
    List<RegistrationStatus> findByAdminVerifiedFalse();
    
    void deleteByUserAndStage(User user, RegistrationStage stage);
}
