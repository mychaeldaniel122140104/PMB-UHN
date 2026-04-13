package com.uhn.pmb.task;

import com.uhn.pmb.entity.AdmissionForm;
import com.uhn.pmb.entity.FormValidation;
import com.uhn.pmb.entity.Student;
import com.uhn.pmb.repository.AdmissionFormRepository;
import com.uhn.pmb.repository.FormValidationRepository;
import com.uhn.pmb.repository.StudentRepository;
import com.uhn.pmb.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ✅ DAILY REMINDER TASK
 * 
 * Berjalan setiap hari (default: 08:00 AM) untuk mengirim auto-reminders ke mahasiswa:
 * 1. PENDING VALIDATION: "Silakan lengkapi formulir"
 * 2. NOT PAID: "Segera lakukan pembayaran"
 * 3. APPROVED (sudah bayar): "Silakan lakukan ujian"
 * 4. INCOMPLETE (belum daftar): "Silakan mulai mendaftar"
 * 
 * Email dikirim OTOMATIS tanpa perlu admin klik tombol
 */
@Component
@EnableScheduling
@Slf4j
public class DailyReminderTask {

    @Autowired
    private FormValidationRepository formValidationRepository;

    @Autowired
    private AdmissionFormRepository admissionFormRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Run daily at 08:00 AM
     * cron = "0 0 8 * * ?" = 08:00 AM setiap hari
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyReminders() {
        try {
            log.info("📅 [DAILY-REMINDER] Starting daily reminder batch job at {}", LocalDateTime.now());

            // 1. Send reminders to PENDING (belum validasi tapi sudah bayar)
            sendPendingReminders();

            // 2. Send reminders to NOT_PAID (sudah daftar, belum bayar)
            sendNotPaidReminders();

            // 3. Send reminders to APPROVED (approved, harus ujian)
            sendApprovedReminders();

            // 4. Send reminders to INCOMPLETE (belum daftar sama sekali)
            sendIncompleteReminders();

            log.info("✅ [DAILY-REMINDER] Daily reminder batch job completed successfully");

        } catch (Exception e) {
            log.error("❌ [DAILY-REMINDER-ERROR] Error in daily reminder task: {}", e.getMessage(), e);
        }
    }

    /**
     * Send reminder to students PENDING validation (paid, waiting admin review)
     */
    private void sendPendingReminders() {
        try {
            List<FormValidation> pendingForms = formValidationRepository.findByValidationStatus(FormValidation.ValidationStatus.PENDING);
            pendingForms = pendingForms.stream()
                    .filter(f -> f.getPaymentStatus() != null && 
                            (f.getPaymentStatus().equals(FormValidation.PaymentStatus.PAID) || f.getPaymentStatus().equals(FormValidation.PaymentStatus.VERIFIED)))
                    .toList();

            int count = 0;
            for (FormValidation fv : pendingForms) {
                try {
                    String studentEmail = fv.getStudent().getUser().getEmail();
                    String studentName = fv.getStudent().getFullName();

                    String subject = "⏳ Reminder: Data Formulir Anda Dalam Proses Verifikasi";
                    String body = buildPendingReminderEmail(studentName);

                    emailService.sendHtmlEmail(studentEmail, subject, body);
                    count++;

                    log.info("  ✉️ PENDING reminder sent to: {}", studentEmail);
                } catch (Exception e) {
                    log.warn("  ⚠️ Failed to send PENDING reminder: {}", e.getMessage());
                }
            }

            log.info("✅ [PENDING-REMINDER] Sent {} reminders", count);
        } catch (Exception e) {
            log.error("❌ [PENDING-REMINDER-ERROR] {}", e.getMessage());
        }
    }

    /**
     * Send reminder to students NOT_PAID (registered but haven't paid)
     */
    private void sendNotPaidReminders() {
        try {
            List<FormValidation> notPaidForms = formValidationRepository.findByValidationStatus(FormValidation.ValidationStatus.PENDING);
            notPaidForms = notPaidForms.stream()
                    .filter(f -> f.getPaymentStatus() == null || f.getPaymentStatus().equals(FormValidation.PaymentStatus.PENDING))
                    .toList();

            int count = 0;
            for (FormValidation fv : notPaidForms) {
                try {
                    String studentEmail = fv.getStudent().getUser().getEmail();
                    String studentName = fv.getStudent().getFullName();

                    String subject = "⚠️ Reminder: Segera Lakukan Pembayaran Pendaftaran";
                    String body = buildNotPaidReminderEmail(studentName);

                    emailService.sendHtmlEmail(studentEmail, subject, body);
                    count++;

                    log.info("  ✉️ NOT_PAID reminder sent to: {}", studentEmail);
                } catch (Exception e) {
                    log.warn("  ⚠️ Failed to send NOT_PAID reminder: {}", e.getMessage());
                }
            }

            log.info("✅ [NOT_PAID-REMINDER] Sent {} reminders", count);
        } catch (Exception e) {
            log.error("❌ [NOT_PAID-REMINDER-ERROR] {}", e.getMessage());
        }
    }

    /**
     * Send reminder to students APPROVED (form approved, paid, must take exam)
     */
    private void sendApprovedReminders() {
        try {
            List<FormValidation> approvedForms = formValidationRepository.findByValidationStatus(FormValidation.ValidationStatus.APPROVED);
            approvedForms = approvedForms.stream()
                    .filter(f -> f.getPaymentStatus() != null && 
                            (f.getPaymentStatus().equals(FormValidation.PaymentStatus.PAID) || f.getPaymentStatus().equals(FormValidation.PaymentStatus.VERIFIED)))
                    .toList();

            int count = 0;
            for (FormValidation fv : approvedForms) {
                try {
                    String studentEmail = fv.getStudent().getUser().getEmail();
                    String studentName = fv.getStudent().getFullName();

                    String subject = "✅ Reminder: Formulir Disetujui - Lakukan Ujian Online";
                    String body = buildApprovedReminderEmail(studentName);

                    emailService.sendHtmlEmail(studentEmail, subject, body);
                    count++;

                    log.info("  ✉️ APPROVED reminder sent to: {}", studentEmail);
                } catch (Exception e) {
                    log.warn("  ⚠️ Failed to send APPROVED reminder: {}", e.getMessage());
                }
            }

            log.info("✅ [APPROVED-REMINDER] Sent {} reminders", count);
        } catch (Exception e) {
            log.error("❌ [APPROVED-REMINDER-ERROR] {}", e.getMessage());
        }
    }

    /**
     * Send reminder to students INCOMPLETE (haven't registered at all)
     */
    private void sendIncompleteReminders() {
        try {
            // Get all students
            List<Student> allStudents = studentRepository.findAll();

            // Get students who submitted forms (submitted IDs)
            List<FormValidation> allForms = formValidationRepository.findAll();
            Set<Long> submittedStudentIds = new HashSet<>();
            for (FormValidation fv : allForms) {
                submittedStudentIds.add(fv.getStudent().getId());
            }

            // Filter students who haven't submitted
            List<Student> incompleteStudents = allStudents.stream()
                    .filter(s -> !submittedStudentIds.contains(s.getId()))
                    .toList();

            int count = 0;
            for (Student student : incompleteStudents) {
                try {
                    String studentEmail = student.getUser().getEmail();
                    String studentName = student.getFullName();

                    String subject = "📝 Reminder: Segera Lengkapi Pendaftaran Anda";
                    String body = buildIncompleteReminderEmail(studentName);

                    emailService.sendHtmlEmail(studentEmail, subject, body);
                    count++;

                    log.info("  ✉️ INCOMPLETE reminder sent to: {}", studentEmail);
                } catch (Exception e) {
                    log.warn("  ⚠️ Failed to send INCOMPLETE reminder: {}", e.getMessage());
                }
            }

            log.info("✅ [INCOMPLETE-REMINDER] Sent {} reminders", count);
        } catch (Exception e) {
            log.error("❌ [INCOMPLETE-REMINDER-ERROR] {}", e.getMessage());
        }
    }

    // ==================== EMAIL BODY BUILDERS ====================

    private String buildPendingReminderEmail(String studentName) {
        return "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<h2>⏳ Reminder: Data Formulir Anda Dalam Proses Verifikasi</h2>" +
                "<p>Halo " + studentName + ",</p>" +
                "<p>Terima kasih telah melakukan pembayaran dan mengisi formulir pendaftaran.</p>" +
                "<p><strong>Data Anda sedang dalam proses verifikasi oleh admin kami.</strong> Kami akan segera melakukan review mendalam terhadap dokumen dan informasi yang Anda berikan.</p>" +
                "<p>Silakan tunggu beberapa hari hingga Anda menerima notifikasi approval atau jika ada revisi yang diperlukan.</p>" +
                "<p>Jika ada pertanyaan, hubungi Customer Service kami.</p>" +
                "<hr>" +
                "<p>Terima kasih,<br/>" +
                "<strong>Admin PMB UHN</strong></p>" +
                "</body></html>";
    }

    private String buildNotPaidReminderEmail(String studentName) {
        return "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<h2>⚠️ Reminder: Segera Lakukan Pembayaran Pendaftaran</h2>" +
                "<p>Halo " + studentName + ",</p>" +
                "<p>Kami notice bahwa Anda telah mengisi formulir pendaftaran tetapi <strong>belum melakukan pembayaran!</strong></p>" +
                "<p style='background: #fff3cd; padding: 15px; border-left: 4px solid #ffc107;'>" +
                "<strong>📌 JANGAN LUPA BAYAR!</strong><br/>" +
                "Silakan segera lakukan pembayaran melalui Virtual Account (VA) yang sudah diberikan sebelumnya. " +
                "Pembayaran adalah persyaratan untuk melanjutkan proses pendaftaran Anda." +
                "</p>" +
                "<p><strong>Batas waktu pembayaran:</strong> 7 hari sejak pendaftaran<br/>" +
                "<strong>Jika sudah membayar:</strong> Tunggu hingga 1x24 jam untuk verifikasi otomatis.</p>" +
                "<p>Jika ada masalah atau butuh bantuan, segera hubungi Customer Service kami.</p>" +
                "<hr>" +
                "<p>Terima kasih,<br/>" +
                "<strong>Admin PMB UHN</strong></p>" +
                "</body></html>";
    }

    private String buildApprovedReminderEmail(String studentName) {
        return "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<h2>✅ Reminder: Formulir Disetujui - Lakukan Ujian Online</h2>" +
                "<p>Halo " + studentName + ",</p>" +
                "<p style='background: #d4edda; padding: 15px; border-left: 4px solid #28a745;'>" +
                "<strong>🎉 SELAMAT!</strong><br/>" +
                "Formulir pendaftaran Anda telah <strong>DISETUJUI</strong> oleh admin kami. Pembayaran Anda juga sudah OK." +
                "</p>" +
                "<p style='font-size: 1.1em; font-weight: bold;'>📝 NEXT STEP: UJIAN ONLINE</p>" +
                "<p>Anda sekarang berhak untuk mengakses ujian online kami. <strong>Token akses sudah dikirim ke email Anda.</strong></p>" +
                "<ol>" +
                "<li>Cek email untuk token ujian (format: <code>UHN-TOKEN-XXXXX</code>)</li>" +
                "<li>Login ke dashboard dan masukkan token</li>" +
                "<li>Kerjakan ujian online sesuai waktu yang tersedia</li>" +
                "</ol>" +
                "<p><strong>⏱️ Token berlaku selama 2 jam setelah aktivasi.</strong></p>" +
                "<p>Jika belum menerima token atau ada pertanyaan, hubungi Customer Service kami.</p>" +
                "<hr>" +
                "<p>Jelas!,<br/>" +
                "<strong>Admin PMB UHN</strong></p>" +
                "</body></html>";
    }

    private String buildIncompleteReminderEmail(String studentName) {
        return "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                "<h2>📝 Reminder: Segera Lengkapi Pendaftaran Anda</h2>" +
                "<p>Halo " + studentName + ",</p>" +
                "<p>Kami notice bahwa Anda <strong>belum menyelesaikan proses pendaftaran.</strong></p>" +
                "<p style='background: #f8d7da; padding: 15px; border-left: 4px solid #dc3545;'>" +
                "<strong>⏰ SEGERA DAFTAR!</strong><br/>" +
                "Untuk melanjutkan proses penerimaan, silakan ikuti langkah-langkah berikut:" +
                "</p>" +
                "<ol>" +
                "<li>Login ke sistem pendaftaran kami</li>" +
                "<li>Isi formulir lengkap dengan data yang benar dan jujur</li>" +
                "<li>Unggah dokumen yang diperlukan (scan ijazah, KTP, foto, dll)</li>" +
                "<li>Submit formulir</li>" +
                "<li>Lakukan pembayaran sesuai instruksi</li>" +
                "</ol>" +
                "<p><strong>Batas waktu pendaftaran:</strong> [CEK TANGGAL]</p>" +
                "<p>Jika ada kendala atau butuh panduan, jangan ragu untuk menghubungi Customer Service kami.</p>" +
                "<hr>" +
                "<p>Terima kasih,<br/>" +
                "<strong>Admin PMB UHN</strong></p>" +
                "</body></html>";
    }
}
