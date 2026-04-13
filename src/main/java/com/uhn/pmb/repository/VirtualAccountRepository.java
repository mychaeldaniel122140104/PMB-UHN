package com.uhn.pmb.repository;

import com.uhn.pmb.entity.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, Long> {
    Optional<VirtualAccount> findByVaNumber(String vaNumber);
    List<VirtualAccount> findByStudent_Id(Long studentId);
    List<VirtualAccount> findByStatus(VirtualAccount.VAStatus status);
    List<VirtualAccount> findByStudent_IdAndStatus(Long studentId, VirtualAccount.VAStatus status);
}
