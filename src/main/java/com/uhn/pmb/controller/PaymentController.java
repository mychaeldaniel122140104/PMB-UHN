package com.uhn.pmb.controller;

import com.uhn.pmb.dto.ManualPaymentDTO;
import com.uhn.pmb.dto.ManualPaymentSubmitRequest;
import com.uhn.pmb.dto.UniversityBankAccountDTO;
import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private ManualPaymentRepository manualPaymentRepository;

    @Autowired
    private UniversityBankAccountRepository universityBankAccountRepository;

    @Autowired
    private AdmissionFormRepository admissionFormRepository;

    private static final String UPLOAD_DIR = "uploads/payment-proof/";

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

    // ==================== SUBMIT MANUAL PAYMENT WITH PROOF ====================

    /**
     * POST /api/payment/manual/submit
     * Submit pembayaran manual dengan bukti transfer
     * 
     * Form Data:
     * - admissionFormId: Long
     * - amount: Long
     * - bankName: String
     * - accountHolder: String
     * - transactionDate: String (ISO format)
     * - proofFile: MultipartFile (image)
     */
    @PostMapping("/manual/submit")
    public ResponseEntity<Map<String, Object>> submitManualPayment(
            @RequestParam Long admissionFormId,
            @RequestParam Long amount,
            @RequestParam String bankName,
            @RequestParam String accountHolder,
            @RequestParam String transactionDate,
            @RequestParam("proofFile") MultipartFile proofFile) {
        
        try {
            // Validate admission form exists
            Optional<AdmissionForm> admissionFormOpt = admissionFormRepository.findById(admissionFormId);
            if (admissionFormOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Formulir pendaftaran tidak ditemukan"
                ));
            }

            AdmissionForm form = admissionFormOpt.get();

            // Check if manual payment already exists
            Optional<ManualPayment> existingPayment = manualPaymentRepository.findByAdmissionFormId(admissionFormId);
            if (existingPayment.isPresent()) {
                ManualPayment existing = existingPayment.get();
               if (!existing.getStatus().equals(ManualPayment.PaymentStatus.REJECTED)) {
                    return ResponseEntity.status(400).body(Map.of(
                            "success", false,
                            "message", "Pembayaran manual sudah pernah diajukan untuk formulir ini"
                    ));
                }
            }

            // Save the proof image
            String fileName = "payment_" + admissionFormId + "_" + System.currentTimeMillis() + ".jpg";
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, proofFile.getBytes());

            // Create or update ManualPayment
            ManualPayment payment = existingPayment.orElse(new ManualPayment());
            payment.setAdmissionForm(form);
            payment.setAmount(amount);
            payment.setBankName(bankName);
            payment.setAccountHolder(accountHolder);
            payment.setTransactionDate(LocalDateTime.parse(transactionDate));
            payment.setProofImagePath(fileName);
            payment.setStatus(ManualPayment.PaymentStatus.PENDING);
            payment.setPaymentMethod(ManualPayment.PaymentMethod.MANUAL);

            ManualPayment saved = manualPaymentRepository.save(payment);

            // Update AdmissionForm payment method
            form.setPaymentMethod(AdmissionForm.PaymentMethod.MANUAL);
            admissionFormRepository.save(form);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bukti pembayaran berhasil diunggah, menunggu verifikasi admin",
                    "paymentId", saved.getId(),
                    "status", saved.getStatus().getLabel()
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Gagal menyimpan file bukti pembayaran: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ==================== GET MANUAL PAYMENT STATUS ====================

    /**
     * GET /api/payment/manual/{id}
     * Mendapatkan status pembayaran manual
     */
    @GetMapping("/manual/{id}")
    public ResponseEntity<ManualPaymentDTO> getManualPaymentStatus(@PathVariable Long id) {
        try {
            Optional<ManualPayment> paymentOpt = manualPaymentRepository.findById(id);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(404).build();
            }

            ManualPayment payment = paymentOpt.get();
            ManualPaymentDTO dto = ManualPaymentDTO.builder()
                    .id(payment.getId())
                    .admissionFormId(payment.getAdmissionForm().getId())
                    .amount(payment.getAmount())
                    .bankName(payment.getBankName())
                    .accountHolder(payment.getAccountHolder())
                    .transactionDate(payment.getTransactionDate())
                    .status(payment.getStatus().getLabel())
                    .proofImageUrl("/uploads/payment-proof/" + payment.getProofImagePath())
                    .rejectionReason(payment.getRejectionReason())
                    .createdAt(payment.getCreatedAt())
                    .verifiedAt(payment.getVerifiedAt())
                    .verifiedBy(payment.getVerifiedBy())
                    .build();

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/payment/manual/admission-form/{admissionFormId}
     * Mendapatkan status pembayaran manual berdasarkan admission form ID
     */
    @GetMapping("/manual/admission-form/{admissionFormId}")
    public ResponseEntity<ManualPaymentDTO> getManualPaymentByAdmissionForm(@PathVariable Long admissionFormId) {
        try {
            Optional<ManualPayment> paymentOpt = manualPaymentRepository.findByAdmissionFormId(admissionFormId);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(404).build();
            }

            ManualPayment payment = paymentOpt.get();
            ManualPaymentDTO dto = ManualPaymentDTO.builder()
                    .id(payment.getId())
                    .admissionFormId(payment.getAdmissionForm().getId())
                    .amount(payment.getAmount())
                    .bankName(payment.getBankName())
                    .accountHolder(payment.getAccountHolder())
                    .transactionDate(payment.getTransactionDate())
                    .status(payment.getStatus().getLabel())
                    .proofImageUrl("/uploads/payment-proof/" + payment.getProofImagePath())
                    .rejectionReason(payment.getRejectionReason())
                    .createdAt(payment.getCreatedAt())
                    .verifiedAt(payment.getVerifiedAt())
                    .verifiedBy(payment.getVerifiedBy())
                    .build();

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
