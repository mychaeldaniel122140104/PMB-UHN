package com.uhn.pmb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uhn.pmb.entity.VirtualAccount;
import com.uhn.pmb.repository.VirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BrivaService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final ObjectMapper objectMapper;

    @Value("${briva.api.key}")
    private String brivaApiKey;

    @Value("${briva.api.secret}")
    private String brivaApiSecret;

    @Value("${briva.api.url}")
    private String brivaApiUrl;

    @Value("${briva.client.id}")
    private String brivaClientId;

    /**
     * Generate Virtual Account for payment
     */
    public String generateVirtualAccount(VirtualAccount va) throws IOException {
        try {
            // Simulate BRIVA API call
            // In production, you would call the actual BRIVA API endpoint
            
            String vaNumber = generateUniqueVANumber();
            va.setVaNumber(vaNumber);
            va.setBrivaReference(UUID.randomUUID().toString());
            va.setExpiredAt(LocalDateTime.now().plusDays(7)); // 7 days expiry
            va.setStatus(VirtualAccount.VAStatus.ACTIVE);
            
            VirtualAccount saved = virtualAccountRepository.save(va);
            log.info("Virtual Account generated: {}", vaNumber);
            
            return vaNumber;
        } catch (Exception e) {
            log.error("Error generating virtual account: {}", e.getMessage());
            throw new RuntimeException("Gagal membuat Virtual Account: " + e.getMessage());
        }
    }

    /**
     * Verify payment status with BRIVA
     */
    public boolean verifyPayment(String vaNumber) {
        try {
            // In production, call BRIVA API to verify payment
            // This is a mock implementation
            
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("VA not found"));
            
            // Check if payment has been made (in real scenario, BRIVA callback would handle this)
            if (va.getStatus() == VirtualAccount.VAStatus.PAID) {
                log.info("Payment verified for VA: {}", vaNumber);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error verifying payment: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Update payment status based on callback from BRIVA
     */
    public void updatePaymentStatus(String vaNumber, String transactionId, BigDecimal amount) {
        try {
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("VA not found"));

            if (va.getAmount().equals(amount)) {
                va.setStatus(VirtualAccount.VAStatus.PAID);
                va.setPaidAt(LocalDateTime.now());
                va.setBrivaReference(transactionId);
                virtualAccountRepository.save(va);
                
                log.info("Payment status updated to PAID for VA: {}", vaNumber);
            } else {
                log.warn("Amount mismatch for VA: {} - Expected: {}, Got: {}", 
                        vaNumber, va.getAmount(), amount);
            }
        } catch (Exception e) {
            log.error("Error updating payment status: {}", e.getMessage());
        }
    }

    /**
     * Cancel Virtual Account
     */
    public void cancelVirtualAccount(String vaNumber) {
        try {
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("VA not found"));

            if (va.getStatus() != VirtualAccount.VAStatus.PAID) {
                va.setStatus(VirtualAccount.VAStatus.CANCELLED);
                virtualAccountRepository.save(va);
                log.info("Virtual Account cancelled: {}", vaNumber);
            }
        } catch (Exception e) {
            log.error("Error cancelling virtual account: {}", e.getMessage());
        }
    }

    /**
     * Get VA details
     */
    public Map<String, Object> getVADetails(String vaNumber) {
        VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                .orElseThrow(() -> new RuntimeException("VA not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("vaNumber", va.getVaNumber());
        details.put("amount", va.getAmount());
        details.put("status", va.getStatus());
        details.put("expiredAt", va.getExpiredAt());
        details.put("paidAt", va.getPaidAt());
        details.put("paymentType", va.getPaymentType());
        
        return details;
    }

    /**
     * Generate unique VA number
     */
    private String generateUniqueVANumber() {
        String vaNumber;
        do {
            // Format: 8860[6-digit-sequence][6-digit-studentid]
            long timestamp = System.currentTimeMillis() % 1000000;
            long random = (long) (Math.random() * 1000000);
            vaNumber = String.format("8860%06d%06d", timestamp, random);
        } while (virtualAccountRepository.findByVaNumber(vaNumber).isPresent());
        
        return vaNumber;
    }

    /**
     * Inquiry VA with BRIVA (mock implementation)
     */
    public Map<String, Object> inquiryVA(String vaNumber) throws IOException {
        try {
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("VA not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("vaNumber", va.getVaNumber());
            response.put("amount", va.getAmount());
            response.put("status", va.getStatus());
            response.put("expiredAt", va.getExpiredAt().format(DateTimeFormatter.ISO_DATE_TIME));
            
            return response;
        } catch (Exception e) {
            log.error("Error in VA inquiry: {}", e.getMessage());
            throw new IOException("VA inquiry failed", e);
        }
    }
}
