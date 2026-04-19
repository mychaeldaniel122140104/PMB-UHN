package com.uhn.pmb.controller;

import com.uhn.pmb.dto.UniversityBankAccountDTO;
import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private UniversityBankAccountRepository universityBankAccountRepository;

    // ==================== GET BANK ACCOUNTS ====================
    
    /**
     * GET /api/payment/bank-accounts
     * Mendapatkan daftar rekening universitas yang aktif
     */
    @GetMapping("/bank-accounts")
    public ResponseEntity<List<UniversityBankAccountDTO>> getActiveBankAccounts() {
        try {
            List<UniversityBankAccount> accounts = universityBankAccountRepository.findByIsActiveTrueOrderByBankName();
            List<UniversityBankAccountDTO> dtos = accounts.stream()
                    .map(acc -> UniversityBankAccountDTO.builder()
                            .id(acc.getId())
                            .bankName(acc.getBankName())
                            .accountNumber(acc.getAccountNumber())
                            .accountHolder(acc.getAccountHolder())
                            .purpose(acc.getPurpose())
                            .isActive(acc.getIsActive())
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}
