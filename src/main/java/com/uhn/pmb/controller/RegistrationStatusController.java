package com.uhn.pmb.controller;

import com.uhn.pmb.entity.RegistrationStatus;
import com.uhn.pmb.entity.RegistrationStatus.RegistrationStage;
import com.uhn.pmb.entity.User;
import com.uhn.pmb.repository.UserRepository;
import com.uhn.pmb.service.RegistrationStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/camaba/registration-status")
@CrossOrigin(origins = "*")
@Slf4j
public class RegistrationStatusController {
    
    @Autowired
    private RegistrationStatusService registrationStatusService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get semua status registrasi user dengan computed fields
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllStatuses(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User tidak ditemukan"
                ));
            }
            
            User user = userOpt.get();
            List<RegistrationStatus> statuses = registrationStatusService.getUserStatuses(user);
            
            // Enhance each status with computed fields for frontend
            List<Map<String, Object>> enhancedStatuses = new java.util.ArrayList<>();
            for (RegistrationStatus status : statuses) {
                Map<String, Object> statusMap = new HashMap<>();
                statusMap.put("id", status.getId());
                statusMap.put("stage", status.getStage().toString());
                statusMap.put("status", status.getStatus().toString());
                statusMap.put("submissionDate", status.getSubmissionDate());
                statusMap.put("createdAt", status.getCreatedAt());
                statusMap.put("updatedAt", status.getUpdatedAt());
                statusMap.put("editDeadline", status.getEditDeadline());
                statusMap.put("canEdit", status.getCanEdit());
                statusMap.put("adminVerified", status.getAdminVerified());
                statusMap.put("verifiedBy", status.getVerifiedBy());
                statusMap.put("verificationDate", status.getVerificationDate());
                statusMap.put("adminNotes", status.getAdminNotes());
                statusMap.put("editCount", status.getEditCount());
                
                // Add computed field: remaining hours for editing
                Long editTimeRemaining = registrationStatusService.getEditTimeRemaining(user, status.getStage());
                statusMap.put("editTimeRemainingHours", editTimeRemaining);
                
                enhancedStatuses.add(statusMap);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enhancedStatuses
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get status untuk stage tertentu (parse String ke enum)
     */
    @GetMapping("/{stage}")
    public ResponseEntity<?> getStatus(@PathVariable String stage, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User tidak ditemukan"
                ));
            }
            
            User user = userOpt.get();
            RegistrationStage registrationStage = RegistrationStage.valueOf(stage.toUpperCase());
            Optional<RegistrationStatus> statusOpt = registrationStatusService.getStatus(user, registrationStage);
            
            if (statusOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Status tidak ditemukan"
                ));
            }
            
            RegistrationStatus regStatus = statusOpt.get();
            boolean canEdit = registrationStatusService.canUserEdit(user, registrationStage);
            Long timeRemaining = registrationStatusService.getEditTimeRemaining(user, registrationStage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", regStatus);
            response.put("canEdit", canEdit);
            response.put("editTimeRemainingHours", timeRemaining);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Stage tidak valid: " + stage
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Mark tahapan sebagai selesai/completed
     */
    @PostMapping("/{stage}/complete")
    public ResponseEntity<?> completeStage(
            @PathVariable String stage,
            @RequestBody(required = false) Map<String, Object> request,
            Authentication authentication) {
        try {
            log.info("📝 [COMPLETE-STAGE] Received request for stage: {}", stage);
            
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                log.error("❌ User not found: {}", email);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User tidak ditemukan"
                ));
            }
            
            User user = userOpt.get();
            log.info("✓ User found: {} (ID: {})", email, user.getId());
            
            RegistrationStage registrationStage = RegistrationStage.valueOf(stage.toUpperCase());
            log.info("✓ Stage enum converted: {}", registrationStage);
            
            // Handle null request body - Convert entire request to JSON, OR extract from "data" key
            String dataJson = "";
            if (request != null) {
                // ✅ FIXED: First try to get "data" key (for formula-selection format)
                if (request.containsKey("data")) {
                    Object data = request.get("data");
                    dataJson = data != null ? data.toString() : "";
                    log.info("  Extracted from 'data' key");
                } else {
                    // ✅ FALLBACK: Convert entire request body to JSON (for gelombang-selection format)
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        dataJson = mapper.writeValueAsString(request);
                        log.info("  Converted entire request body to JSON");
                    } catch (Exception e) {
                        log.warn("  Could not convert request to JSON: {}", e.getMessage());
                    }
                }
            }
            log.info("✓ Data JSON extracted, length: {}", dataJson.length());
            log.info("  Data content: {}", dataJson.substring(0, Math.min(200, dataJson.length())));
            
            RegistrationStatus status = registrationStatusService.markAsCompleted(user, registrationStage, dataJson);
            log.info("✅ Registration status marked as completed: {}", status.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tahapan " + stage + " berhasil diselesaikan");
            response.put("data", status);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid stage: {} - {}", stage, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Stage tidak valid: " + stage
            ));
        } catch (Exception e) {
            log.error("❌ Error completing stage {}: {}", stage, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Update data tahapan (hanya jika masih bisa edit)
     */
    @PutMapping("/{stage}/update")
    public ResponseEntity<?> updateStageData(
            @PathVariable String stage,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User tidak ditemukan"
                ));
            }
            
            User user = userOpt.get();
            RegistrationStage registrationStage = RegistrationStage.valueOf(stage.toUpperCase());
            
            boolean canEdit = registrationStatusService.canUserEdit(user, registrationStage);
            if (!canEdit) {
                Long timeRemaining = registrationStatusService.getEditTimeRemaining(user, registrationStage);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Waktu edit sudah habis (24 jam telah lewat atau sudah diverifikasi admin)",
                    "editTimeRemainingHours", timeRemaining
                ));
            }
            
            String dataJson = request.getOrDefault("data", "").toString();
            RegistrationStatus status = registrationStatusService.updateStatusData(user, registrationStage, dataJson);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Data berhasil diupdate",
                "data", status
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Stage tidak valid: " + stage
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Check apakah user bisa edit
     */
    @GetMapping("/{stage}/can-edit")
    public ResponseEntity<?> checkCanEdit(@PathVariable String stage, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User tidak ditemukan"
                ));
            }
            
            User user = userOpt.get();
            RegistrationStage registrationStage = RegistrationStage.valueOf(stage.toUpperCase());
            boolean canEdit = registrationStatusService.canUserEdit(user, registrationStage);
            Long timeRemaining = registrationStatusService.getEditTimeRemaining(user, registrationStage);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "canEdit", canEdit,
                "editTimeRemainingHours", timeRemaining
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Stage tidak valid: " + stage
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
}
