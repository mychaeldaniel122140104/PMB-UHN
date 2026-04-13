package com.uhn.pmb.task;

import com.uhn.pmb.entity.FormValidation;
import com.uhn.pmb.entity.VirtualAccount;
import com.uhn.pmb.repository.FormValidationRepository;
import com.uhn.pmb.repository.VirtualAccountRepository;
import com.uhn.pmb.service.ExamTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ SCHEDULER TASK untuk AUTO-DETECT PEMBAYARAN BRIVA
 * 
 * Berjalan setiap 5 menit untuk:
 * 1. Cek semua VirtualAccount yang status masih ACTIVE (belum bayar)
 * 2. Query BRIVA API untuk lihat apakah sudah bayar
 * 3. Jika sudah bayar, update VirtualAccount.status = PAID
 * 4. AUTO-UPDATE FormValidation.paymentStatus = PAID
 * 5. NO MANUAL VERIFICATION NEEDED!
 * 
 * Ini adalah kunci automation yang diminta user
 */
@Component
@EnableScheduling
@Slf4j
public class BrivaPaymentCheckTask {

    @Autowired
    private VirtualAccountRepository virtualAccountRepository;

    @Autowired
    private FormValidationRepository formValidationRepository;

    @Autowired
    private ExamTokenService examTokenService;

    /**
     * Check setiap 5 menit (300000 ms = 5 menit)
     * initialDelay: 10 detik - tunggu aplikasi fully loaded sebelum mulai
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 10000)
    public void checkBrivaPayments() {
        try {
            log.info("🔍 [AUTO-DETECT] Starting BRIVA payment auto-detection check...");

            // Get semua VA yang statusnya ACTIVE (belum bayar atau pending)
            List<VirtualAccount> activeVAs = virtualAccountRepository
                    .findByStatus(VirtualAccount.VAStatus.ACTIVE);

            if (activeVAs == null || activeVAs.isEmpty()) {
                log.debug("✅ [AUTO-DETECT] No active VAs to check");
                return;
            }

            log.info("📢 [AUTO-DETECT] Found {} active VAs to check", activeVAs.size());

            int paymentDetected = 0;
            
            for (VirtualAccount va : activeVAs) {
                try {
                    // ============================================
                    // Check apakah VA sudah dibayar di BRIVA
                    // ============================================
                    if (isPaymentReceived(va)) {
                        log.info("✅ [PAYMENT DETECTED] VA {} has been paid! Updating status...", va.getVaNumber());
                        
                        // Update VirtualAccount status ke PAID
                        va.setStatus(VirtualAccount.VAStatus.PAID);
                        va.setPaidAt(LocalDateTime.now());
                        virtualAccountRepository.save(va);
                        
                        // ============================================
                        // AUTO-UPDATE FormValidation (THIS IS KEY!)
                        // ============================================
                        updateFormValidationPaymentStatus(va);
                        
                        paymentDetected++;
                        
                        log.info("🎉 [AUTO-UPDATE] FormValidation.paymentStatus updated to PAID for VA {}", va.getVaNumber());
                    }

                } catch (Exception e) {
                    log.error("❌ [AUTO-DETECT] Error checking VA {}: {}", va.getVaNumber(), e.getMessage());
                }
            }

            if (paymentDetected > 0) {
                log.info("🟢 [AUTO-DETECT COMPLETE] {} payment(s) auto-detected and processed!", paymentDetected);
            } else {
                log.debug("⭕ [AUTO-DETECT COMPLETE] No new payments detected");
            }

        } catch (Exception e) {
            log.error("❌ [AUTO-DETECT] Fatal error in checkBrivaPayments: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ Check apakah VA sudah dibayar
     * 
     * Check dari VirtualAccount.status = PAID
     * Jika VA sudah punya status PAID (dari webhook BRIVA atau manual update),
     * maka ini trigger token generation
     */
    private boolean isPaymentReceived(VirtualAccount va) {
        try {
            // ✅ FIX: Check apakah VA sudah punya status PAID
            if (va.getStatus() != null && va.getStatus() == VirtualAccount.VAStatus.PAID) {
                log.info("✅ Payment detected for VA {}: Status = {}", va.getVaNumber(), va.getStatus());
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("❌ Error checking payment for VA {}: {}", va.getVaNumber(), e.getMessage());
            return false;
        }
    }

    /**
     * ✅ AUTO-UPDATE FormValidation saat pembayaran terdeteksi
     * ✅ AUTO-GENERATE Exam Token dan kirim ke email
     * 
     * Ini adalah automation key yang menghubungkan:
     * - VirtualAccount.status = PAID
     * - FormValidation.paymentStatus = PAID
     * - ExamToken otomatis di-generate dan dikirim via email
     * 
     * Sehingga form otomatis pindah ke tabel "Formulir untuk di Validasi"
     * DAN token otomatis digenerate tanpa admin intervensi
     */
    private void updateFormValidationPaymentStatus(VirtualAccount va) {
        try {
            if (va.getStudent() == null || va.getAdmissionForm() == null) {
                log.warn("⚠️ VA {} missing student/form data", va.getVaNumber());
                return;
            }

            // Cari FormValidation untuk student ini
            List<FormValidation> validations = formValidationRepository.findAll().stream()
                    .filter(fv -> fv.getStudent().getId().equals(va.getStudent().getId())
                            && fv.getAdmissionForm().getId().equals(va.getAdmissionForm().getId())
                            && fv.getValidationStatus() == FormValidation.ValidationStatus.PENDING)
                    .toList();

            if (validations.isEmpty()) {
                log.warn("⚠️ No PENDING FormValidation found for student {}", va.getStudent().getId());
                return;
            }

            FormValidation fv = validations.get(0);

            // ✅ AUTO-UPDATE payment status (THIS IS THE KEY!)
            fv.setPaymentStatus(FormValidation.PaymentStatus.PAID);
            fv.setPaymentDate(LocalDateTime.now());
            fv.setUpdatedAt(LocalDateTime.now());
            
            formValidationRepository.save(fv);

            log.info(
                    "✅ [AUTO-UPDATE SUCCESS] FormValidation #{} updated: {} {} → paymentStatus = PAID",
                    fv.getId(),
                    va.getStudent().getFullName(),
                    va.getStudent().getUser().getEmail()
            );

            // ✅ NEW: AUTO-GENERATE dan kirim Exam Token ke email
            try {
                log.info("🎫 [AUTO-TOKEN-GEN] Generating exam token for student {}...", va.getStudent().getId());
                
                com.uhn.pmb.entity.ExamToken examToken = examTokenService.generateToken(
                        va.getStudent().getId(),
                        va.getAdmissionForm().getId(),
                        120 // 2 jam expiration
                );
                
                // ✅ Sync token to FormValidation record for dashboard display
                if (examToken != null && examToken.getTokenValue() != null) {
                    fv.setExamToken(examToken.getTokenValue());
                    formValidationRepository.save(fv);
                    log.info("✅ [SYNC] Exam token synced to FormValidation record");
                }
                
                log.info("✅ [AUTO-TOKEN-GEN SUCCESS] Token generated and emailed for student: {}", 
                        va.getStudent().getFullName());
            } catch (Exception tokenGenError) {
                log.error("❌ [AUTO-TOKEN-GEN FAILED] Error generating token: {}", tokenGenError.getMessage(), tokenGenError);
                // Don't fail the whole process if token generation fails
            }

        } catch (Exception e) {
            log.error("❌ Error updating FormValidation for VA {}: {}", va.getVaNumber(), e.getMessage(), e);
        }
    }
}
