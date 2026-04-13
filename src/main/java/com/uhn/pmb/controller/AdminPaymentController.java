package com.uhn.pmb.controller;

import com.uhn.pmb.dto.ManualPaymentDTO;
import com.uhn.pmb.entity.ManualPayment;
import com.uhn.pmb.entity.ManualCicilanPayment;
import com.uhn.pmb.entity.CicilanRequest;
import com.uhn.pmb.repository.ManualPaymentRepository;
import com.uhn.pmb.repository.ManualCicilanPaymentRepository;
import com.uhn.pmb.repository.CicilanRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin API untuk verifikasi pembayaran manual
 * Endpoint untuk admin validasi dalam dashboard-admin-validasi
 */
@Slf4j
@RestController
@RequestMapping("/admin/payment")
@CrossOrigin(origins = "*")
public class AdminPaymentController {

    @Autowired
    private ManualPaymentRepository manualPaymentRepository;

    @Autowired
    private ManualCicilanPaymentRepository manualCicilanPaymentRepository;
    
    @Autowired
    private CicilanRequestRepository cicilanRequestRepository;

    // ==================== GET PENDING PAYMENTS ====================

    /**
     * GET /admin/payment/manual/pending
     * Mendapatkan daftar pembayaran manual yang pending verifikasi
     */
    @GetMapping("/manual/pending")
    public ResponseEntity<List<ManualPaymentDTO>> getPendingPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ManualPayment> payments = manualPaymentRepository.findByStatus(ManualPayment.PaymentStatus.PENDING, pageable);

            List<ManualPaymentDTO> dtos = payments.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // ==================== VERIFY PAYMENT ====================

    /**
     * PUT /admin/payment/manual/{id}/verify
     * Admin verifikasi pembayaran manual (tandai sebagai VERIFIED)
     */
    @PutMapping("/manual/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @PathVariable(required = false) Long id,
            @RequestParam String verifiedBy) {
        try {
            Optional<ManualPayment> paymentOpt = manualPaymentRepository.findById(id);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Pembayaran tidak ditemukan"
                ));
            }

            ManualPayment payment = paymentOpt.get();
            payment.setStatus(ManualPayment.PaymentStatus.VERIFIED);
            payment.setVerifiedBy(verifiedBy);
            payment.setVerifiedAt(LocalDateTime.now());

            manualPaymentRepository.save(payment);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pembayaran berhasil diverifikasi",
                    "status", payment.getStatus().getLabel()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ==================== REJECT PAYMENT ====================

    /**
     * PUT /admin/payment/manual/{id}/reject
     * Admin menolak pembayaran manual (tandai sebagai REJECTED dengan alasan)
     */
    @PutMapping("/manual/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectPayment(
            @PathVariable(required = false) Long id,
            @RequestParam String reason,
            @RequestParam String rejectedBy) {
        try {
            Optional<ManualPayment> paymentOpt = manualPaymentRepository.findById(id);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Pembayaran tidak ditemukan"
                ));
            }

            ManualPayment payment = paymentOpt.get();
            payment.setStatus(ManualPayment.PaymentStatus.REJECTED);
            payment.setRejectionReason(reason);
            payment.setVerifiedBy(rejectedBy);
            payment.setVerifiedAt(LocalDateTime.now());

            manualPaymentRepository.save(payment);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pembayaran berhasil ditolak",
                    "status", payment.getStatus().getLabel()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ==================== GET PAYMENT DETAILS ====================

    /**
     * GET /admin/payment/manual/admission-form/{admissionFormId}
     * Get manual payment details by admission form ID
     */
    @GetMapping("/manual/admission-form/{admissionFormId}")
    public ResponseEntity<?> getManualPaymentByAdmissionForm(@PathVariable Long admissionFormId) {
        try {
            Optional<ManualPayment> paymentOpt = manualPaymentRepository.findByAdmissionFormId(admissionFormId);
            
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Pembayaran manual tidak ditemukan"
                ));
            }

            ManualPayment payment = paymentOpt.get();
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", payment.getId());
            dataMap.put("amount", payment.getAmount());
            dataMap.put("bankName", payment.getBankName());
            dataMap.put("accountHolder", payment.getAccountHolder());
            dataMap.put("status", payment.getStatus().toString());
            dataMap.put("proofImagePath", payment.getProofImagePath());
            dataMap.put("transactionDate", payment.getTransactionDate());
            dataMap.put("rejectionReason", payment.getRejectionReason());
            dataMap.put("createdAt", payment.getCreatedAt());
            dataMap.put("verifiedAt", payment.getVerifiedAt());
            dataMap.put("verifiedBy", payment.getVerifiedBy());
            dataMap.put("student", Map.of(
                    "id", payment.getAdmissionForm().getStudent().getId(),
                    "fullName", payment.getAdmissionForm().getStudent().getFullName()
            ));
            dataMap.put("admissionForm", Map.of(
                    "id", payment.getAdmissionForm().getId()
            ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", dataMap);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /admin/payment/cicilan/student/{studentId}
     * Get cicilan payment details by student ID (latest cicilan)
     * Falls back to CicilanRequest if ManualCicilanPayment not found
     */
    @GetMapping("/cicilan/student/{studentId}")
    public ResponseEntity<?> getCicilanPaymentByStudent(@PathVariable Long studentId) {
        try {
            // ✅ TRY 1: Get latest cicilan payment for this student
            List<ManualCicilanPayment> payments = manualCicilanPaymentRepository.findByStudent_Id(studentId);
            
            if (!payments.isEmpty()) {
                // Get the most recent one
                ManualCicilanPayment payment = payments.stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .findFirst()
                        .orElse(null);

                if (payment != null) {
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("id", payment.getId());
                    dataMap.put("cicilanKe", payment.getCicilanKe());
                    dataMap.put("nominal", payment.getNominal());
                    dataMap.put("status", payment.getStatus().toString());
                    dataMap.put("paymentProofPath", payment.getPaymentProofPath());
                    dataMap.put("keterangan", payment.getKeterangan() != null ? payment.getKeterangan() : "");
                    dataMap.put("createdAt", payment.getCreatedAt());
                    dataMap.put("verifiedAt", payment.getVerifiedAt());
                    dataMap.put("verifiedBy", payment.getVerifiedBy());
                    dataMap.put("student", Map.of(
                            "id", payment.getStudent().getId(),
                            "fullName", payment.getStudent().getFullName()
                    ));
                    
                    // ✅ Include jumlahCicilan from CicilanRequest
                    if (payment.getCicilanRequest() != null && payment.getCicilanRequest().getJumlahCicilan() != null) {
                        dataMap.put("jumlahCicilan", payment.getCicilanRequest().getJumlahCicilan());
                        log.info("✅ [CICILAN-API-FROM-PAYMENT] Including jumlahCicilan: {}", payment.getCicilanRequest().getJumlahCicilan());
                    } else {
                        dataMap.put("jumlahCicilan", 1);
                        log.warn("⚠️ [CICILAN-API-FROM-PAYMENT] CicilanRequest or jumlahCicilan is null, defaulting to 1");
                    }
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("data", dataMap);
                    return ResponseEntity.ok(response);
                }
            }
            
            // ✅ TRY 2: Fallback to CicilanRequest directly if ManualCicilanPayment not found
            List<CicilanRequest> cicilanRequests = cicilanRequestRepository.findByStudentId(studentId);
            
            if (!cicilanRequests.isEmpty()) {
                // Get the most recent one
                CicilanRequest cicilanRequest = cicilanRequests.stream()
                        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .findFirst()
                        .orElse(null);
                
                if (cicilanRequest != null) {
                    Map<String, Object> dataMap = new HashMap<>();
                    dataMap.put("id", cicilanRequest.getId());
                    dataMap.put("status", cicilanRequest.getStatus() != null ? cicilanRequest.getStatus().toString() : "PENDING");
                    dataMap.put("createdAt", cicilanRequest.getCreatedAt());
                    
                    // ✅ Include jumlahCicilan from CicilanRequest directly
                    if (cicilanRequest.getJumlahCicilan() != null) {
                        dataMap.put("jumlahCicilan", cicilanRequest.getJumlahCicilan());
                        log.info("✅ [CICILAN-API-FROM-REQUEST] Including jumlahCicilan from direct request: {}", cicilanRequest.getJumlahCicilan());
                    } else {
                        dataMap.put("jumlahCicilan", 1);
                        log.warn("⚠️ [CICILAN-API-FROM-REQUEST] CicilanRequest jumlahCicilan is null, defaulting to 1");
                    }
                    
                    if (cicilanRequest.getStudent() != null) {
                        dataMap.put("student", Map.of(
                                "id", cicilanRequest.getStudent().getId(),
                                "fullName", cicilanRequest.getStudent().getFullName()
                        ));
                    }
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("data", dataMap);
                    log.info("📢 [CICILAN-API] Returning cicilan data from CicilanRequest (no ManualCicilanPayment found)");
                    return ResponseEntity.ok(response);
                }
            }
            
            // ✅ If no data found in either table
            log.warn("⚠️ [CICILAN-API] No cicilan data found for student_id: {}", studentId);
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Pembayaran cicilan tidak ditemukan"
            ));
        } catch (Exception e) {
            log.error("❌ [CICILAN-API] Error retrieving cicilan data: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    // ==================== HELPER METHODS ====================

    private ManualPaymentDTO convertToDTO(ManualPayment payment) {
        return ManualPaymentDTO.builder()
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
    }
}
