package com.uhn.pmb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

/**
 * WhatsApp Notification Service
 * Kirim notifikasi WhatsApp ke admin untuk event penting
 * 
 * CATATAN: Implementasi saat ini menggunakan placeholder.
 * Untuk production, integrasi dengan Twilio, MessageBird, atau WhatsApp Business API
 */
@Service
@Slf4j
public class WhatsAppService {

    // Admin WhatsApp number
    private static final String ADMIN_PHONE = "083872746279";
    private static final String ADMIN_PHONE_INTERNATIONAL = "+6283872746279";

    /**
     * Kirim notifikasi pending form ke admin
     * Jika ada form yang pending lebih dari 15 menit, kirim pesan ke admin
     */
    public boolean sendPendingFormReminder(String studentName, String studentEmail, 
                                          String submitTime, String formId) {
        try {
            String message = String.format(
                    "🔔 *REMINDER PENDING FORM* 🔔\n\n" +
                    "Ada formulir yang belum divalidasi selama > 15 menit!\n\n" +
                    "📋 *DETAIL:*\n" +
                    "Nama: %s\n" +
                    "Email: %s\n" +
                    "Waktu Submit: %s\n" +
                    "Form ID: %s\n\n" +
                    "⚡ Segera periksa dan validasi di dashboard admin!",
                    studentName, studentEmail, submitTime, formId
            );

            return sendWhatsAppMessage(ADMIN_PHONE, message);

        } catch (Exception e) {
            log.error("❌ Failed to send pending form reminder: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kirim notifikasi form yang sudah divalidasi ke admin
     */
    public boolean sendFormApprovedNotification(String studentName, String token, String expiresAt) {
        try {
            String message = String.format(
                    "✅ *FORM DIVALIDASI* ✅\n\n" +
                    "Formulir telah disetujui dan token ujian sudah di-generate!\n\n" +
                    "👤 Mahasiswa: %s\n" +
                    "🔐 Token: %s\n" +
                    "⏰ Expires: %s\n\n" +
                    "Email notifikasi dengan token sudah dikirim ke mahasiswa.",
                    studentName, token, expiresAt
            );

            return sendWhatsAppMessage(ADMIN_PHONE, message);

        } catch (Exception e) {
            log.error("❌ Failed to send form approved notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kirim notifikasi student submit ujian ke admin
     */
    public boolean sendExamSubmittedNotification(String studentName, int score, boolean passed) {
        try {
            String status = passed ? "✅ LULUS" : "❌ TIDAK LULUS";
            String message = String.format(
                    "📝 *UJIAN DISUBMIT* 📝\n\n" +
                    "Mahasiswa telah submit ujian!\n\n" +
                    "👤 Mahasiswa: %s\n" +
                    "📊 Nilai: %d / 100\n" +
                    "📈 Status: %s\n\n" +
                    "Periksa dashboard untuk detail lengkap.",
                    studentName, score, status
            );

            return sendWhatsAppMessage(ADMIN_PHONE, message);

        } catch (Exception e) {
            log.error("❌ Failed to send exam submitted notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kirim notifikasi sistem ke admin
     */
    public boolean sendSystemNotification(String title, String message) {
        try {
            String whatsAppMessage = String.format("⚙️ *%s* ⚙️\n\n%s", title, message);
            return sendWhatsAppMessage(ADMIN_PHONE, whatsAppMessage);

        } catch (Exception e) {
            log.error("❌ Failed to send system notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Kirim pesan WhatsApp (base method)
     * 
     * IMPLEMENTASI PLACEHOLDER:
     * Untuk production, gunakan salah satu opsi:
     * 1. Twilio - REST API (recommended)
     * 2. MessageBird - REST API
     * 3. WhatsApp Business API - Direct integration
     * 4. Custom webhook/integration
     */
    private boolean sendWhatsAppMessage(String phoneNumber, String message) {
        try {
            log.info("📱 WhatsApp Message to: {} | Message: {}", phoneNumber, message);
            
            // PLACEHOLDER IMPLEMENTATION
            // TODO: Integrasi dengan Twilio atau provider lain
            
            // Contoh implementasi dengan Twilio:
            // HttpClient client = HttpClient.newHttpClient();
            // HttpRequest request = HttpRequest.newBuilder()
            //    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/{ACCOUNT_SID}/Messages.json"))
            //    .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials))
            //    .POST(HttpRequest.BodyPublishers.ofString(body))
            //    .build();
            // HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            log.info("✅ WhatsApp message sent successfully (PLACEHOLDER)");
            return true;

        } catch (Exception e) {
            log.error("❌ Error sending WhatsApp message: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get admin WhatsApp number (formatted untuk URL)
     */
    public static String getAdminWhatsAppLink() {
        // Format untuk wa.me link
        return "https://wa.me/" + ADMIN_PHONE_INTERNATIONAL.replace("+", "");
    }

    /**
     * Get admin WhatsApp number (raw)
     */
    public static String getAdminPhoneNumber() {
        return ADMIN_PHONE_INTERNATIONAL;
    }

    /**
     * Get admin WhatsApp number (for display)
     */
    public static String getAdminPhoneDisplay() {
        return "+62 838-7274-6279";
    }
}
