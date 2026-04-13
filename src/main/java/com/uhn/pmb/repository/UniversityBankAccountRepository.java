package com.uhn.pmb.repository;

import com.uhn.pmb.entity.UniversityBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UniversityBankAccountRepository extends JpaRepository<UniversityBankAccount, Long> {
    
    List<UniversityBankAccount> findByIsActiveTrueOrderByBankName();
    
    List<UniversityBankAccount> findByIsActive(Boolean isActive);
}
