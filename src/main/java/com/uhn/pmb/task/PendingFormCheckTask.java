package com.uhn.pmb.task;

import com.uhn.pmb.entity.FormValidation;
import com.uhn.pmb.repository.FormValidationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled Task untuk check pending formulir
 * Berjalan setiap 15 menit untuk cek apakah ada form yang pending > 15 menit
 * Jika ada, kirim notifikasi WhatsApp ke admin
 */
@Component
@EnableScheduling
@Slf4j
public class PendingFormCheckTask {

    @Autowired
    private FormValidationRepository formValidationRepository;

    private static final int PENDING_THRESHOLD_MINUTES = 15;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Check setiap 15 menit (900000 ms = 15 menit)
     * initialDelay: delay 30 detik sebelum task pertama berjalan
     */
    @Scheduled(fixedDelay = 900000, initialDelay = 30000)
    public void checkPendingForms() {
        try {
            log.info("🔄 Checking pending forms...");

            // Get semua form yang statusnya PENDING
            List<FormValidation> pendingForms = formValidationRepository
                    .findByValidationStatus(FormValidation.ValidationStatus.PENDING);

            if (pendingForms == null || pendingForms.isEmpty()) {
                log.debug("✅ No pending forms found");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            for (FormValidation form : pendingForms) {
                try {
                    // Hitung berapa lama dari submit ke sekarang
                    long minutesElapsed = java.time.temporal.ChronoUnit.MINUTES
                            .between(form.getCreatedAt(), now);

                    log.debug("Form {} pending for {} minutes", form.getId(), minutesElapsed);

                    // Jika sudah > 15 menit, kirim notifikasi
                    if (minutesElapsed >= PENDING_THRESHOLD_MINUTES) {
                        sendPendingFormNotification(form, minutesElapsed);
                    }

                } catch (Exception e) {
                    log.error("❌ Error processing form {}: {}", form.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error in checkPendingForms task: {}", e.getMessage(), e);
        }
    }

    /**
     * Send notification untuk form yang sudah pending > 15 menit
     */
    private void sendPendingFormNotification(FormValidation form, long minutesElapsed) {
        try {
            String submitTime = form.getCreatedAt() != null 
                    ? form.getCreatedAt().format(FORMATTER) 
                    : "Unknown";

            // Log pending form info
            log.warn("⏳ Pending form detected: ID={}, Status=PENDING, ElapsedMinutes={}, SubmitTime={}",
                    form.getId(), minutesElapsed, submitTime);

        } catch (Exception e) {
            log.error("❌ Error processing pending notification for form {}: {}", form.getId(), e.getMessage(), e);
        }
    }

    /**
     * Alternative: Manual trigger untuk testing
     * Call via: GET /admin/api/test/check-pending-forms
     */
    public void manualCheckPendingForms() {
        log.info("🚀 Manual trigger: Checking pending forms...");
        checkPendingForms();
    }
}
