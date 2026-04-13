package com.uhn.pmb.controller;

import com.uhn.pmb.entity.AdmissionForm;
import com.uhn.pmb.entity.CicilanRequest;
import com.uhn.pmb.entity.ProgramStudi;
import com.uhn.pmb.entity.Student;
import com.uhn.pmb.entity.User;
import com.uhn.pmb.entity.RegistrationStatus;
import com.uhn.pmb.dto.CicilanRequestDTO;
import com.uhn.pmb.repository.AdmissionFormRepository;
import com.uhn.pmb.repository.CicilanRequestRepository;
import com.uhn.pmb.repository.ProgramStudiRepository;
import com.uhn.pmb.repository.StudentRepository;
import com.uhn.pmb.repository.UserRepository;
import com.uhn.pmb.repository.RegistrationStatusRepository;
import com.uhn.pmb.request.CicilanRequestSubmitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cicilan")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CicilanRequestController {

    @Autowired private CicilanRequestRepository cicilanRequestRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AdmissionFormRepository admissionFormRepository;
    @Autowired private ProgramStudiRepository programStudiRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationStatusRepository registrationStatusRepository;

    /**
     * Fetch cicilan request status for current student
     */
    @GetMapping("/status/{admissionFormId}")
    public ResponseEntity<?> getCicilanStatus(@PathVariable Long admissionFormId, Authentication auth) {
        try {
            var cicilan = cicilanRequestRepository.findByAdmissionFormId(admissionFormId);
            if (cicilan.isEmpty()) {
                return ResponseEntity.ok(new ErrorResponse("Belum ada request cicilan"));
            }

            CicilanRequest cr = cicilan.get();
            CicilanRequestDTO dto = convertToDTO(cr);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get cicilan status for authenticated user (no admissionFormId needed)
     * Used when student refresh payment-cicilan page
     */
    @GetMapping("/my-status")
    public ResponseEntity<?> getMyCicilanStatus(Authentication auth) {
        try {
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body(new ErrorResponse("Authentication required"));
            }

            User user = null;
            Object principal = auth.getPrincipal();
            
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof String) {
                String username = (String) principal;
                user = userRepository.findByEmail(username).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(401).body(new ErrorResponse("User not found"));
                }
            } else {
                return ResponseEntity.status(401).body(new ErrorResponse("Unknown authentication type"));
            }

            // Find student by user
            var student = studentRepository.findByUser_Id(user.getId());
            if (student.isEmpty()) {
                return ResponseEntity.status(404).body(new ErrorResponse("Student not found"));
            }

            // Find cicilan request for this student (most recent)
            var cicilans = cicilanRequestRepository.findByStudentId(student.get().getId());
            if (cicilans.isEmpty()) {
                return ResponseEntity.status(404).body(new ErrorResponse("Belum ada request cicilan"));
            }

            // Return the most recent cicilan request
            CicilanRequest cr = cicilans.get(0);  // Already sorted DESC by createdAt in repository
            CicilanRequestDTO dto = convertToDTO(cr);
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Submit cicilan request (Student)
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitCicilanRequest(@RequestBody CicilanRequestSubmitRequest request, Authentication auth) {
        try {
            // ✅ FIX: Safely extract User from authentication
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Authentication required"));
            }
            
            User user = null;
            Object principal = auth.getPrincipal();
            
            if (principal instanceof User) {
                user = (User) principal;
                log.debug("✅ Principal is User object, ID: {}", user.getId());
            } else if (principal instanceof String) {
                // If principal is a String, treat it as username and look it up
                String username = (String) principal;
                log.info("⚠️ Authentication principal is String (username: {}), looking up user...", username);
                user = userRepository.findByEmail(username)
                        .orElse(null);
                if (user == null) {
                    // Try finding by other identifier if available
                    log.warn("❌ User not found by email: {}", username);
                    return ResponseEntity.badRequest().body(new ErrorResponse("User tidak ditemukan: " + username));
                }
                log.info("✅ Found user: {}", user.getId());
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("Unknown authentication type: " + principal.getClass()));
            }
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Validate request
            if (request.getJumlahCicilan() < 1 || request.getJumlahCicilan() > 6) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Jumlah cicilan harus 1-6"));
            }

            // ✅ Validate programStudiId is not null
            if (request.getProgramStudiId() == null || request.getProgramStudiId() <= 0) {
                log.error("❌ Invalid programStudiId: {}", request.getProgramStudiId());
                return ResponseEntity.badRequest().body(new ErrorResponse("Program studi ID tidak valid"));
            }

            // Get program studi
            ProgramStudi programStudi = programStudiRepository.findById(request.getProgramStudiId())
                    .orElseThrow(() -> new RuntimeException("Program studi tidak ditemukan"));

            // ✅ Check if cicilan request already exists for this student
            // (instead of checking by admission form)
            // EXCLUDE REJECTED status - allow resubmit if previously rejected
            var existingCicilan = cicilanRequestRepository.findAll().stream()
                    .filter(cr -> cr.getStudent().getId().equals(student.getId()))
                    .filter(cr -> !cr.getStatus().equals(CicilanRequest.CicilanRequestStatus.REJECTED))
                    .findFirst();
            
            if (existingCicilan.isPresent()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Request cicilan sudah pernah dibuat"));
            }

            // ✅ Try to get admission form if available (useful for reference, but not required)
            AdmissionForm admissionForm = admissionFormRepository.findByStudent_Id(student.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);

            // Create cicilan request
            String paymentMethodStr = request.getPaymentMethod() != null ? request.getPaymentMethod() : "SIMULATION";
            CicilanRequest.PaymentMethod paymentMethod;
            try {
                paymentMethod = CicilanRequest.PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                paymentMethod = CicilanRequest.PaymentMethod.SIMULATION;
            }

            CicilanRequest cicilan = CicilanRequest.builder()
                    .student(student)
                    .programStudi(programStudi)
                    .admissionForm(admissionForm)
                    .jumlahCicilan(request.getJumlahCicilan())
                    .hargaCicilan1(programStudi.getCicilan1() != null ? programStudi.getCicilan1() : 0L)
                    .hargaTotal(programStudi.getHargaTotalPerTahun() != null ? programStudi.getHargaTotalPerTahun() : 0L)
                    .status(CicilanRequest.CicilanRequestStatus.PENDING)
                    .paymentMethod(paymentMethod)
                    .build();

            // Calculate hargaPerCicilan (auto-calculated via trigger, but also in code)
            if (cicilan.getJumlahCicilan() > 0) {
                cicilan.setHargaPerCicilan(cicilan.getHargaTotal() / cicilan.getJumlahCicilan());
            }

            CicilanRequest saved = cicilanRequestRepository.save(cicilan);
            CicilanRequestDTO dto = convertToDTO(saved);

            return ResponseEntity.ok(new SuccessResponse("Request cicilan berhasil disubmit", dto));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Mark cicilan payment as submitted (after manual payment proof upload)
     * Like payment-briva.html, this updates REGISTRATION_STAGES so dashboard detects change
     */
    @PutMapping("/{id}/payment-submitted")
    public ResponseEntity<?> markPaymentSubmitted(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request,
            Authentication auth) {
        try {
            // Safely extract User from authentication
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Authentication required"));
            }
            
            User user = null;
            Object principal = auth.getPrincipal();
            
            if (principal instanceof User) {
                user = (User) principal;
            } else if (principal instanceof String) {
                String username = (String) principal;
                user = userRepository.findByEmail(username)
                        .orElseThrow(() -> new RuntimeException("User tidak ditemukan: " + username));
            } else {
                return ResponseEntity.badRequest().body(new ErrorResponse("Unknown authentication type"));
            }

            // Find cicilan request
            CicilanRequest cicilan = cicilanRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cicilan request tidak ditemukan"));

            // Verify ownership
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student tidak ditemukan"));
            
            if (!cicilan.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Cicilan bukan milik Anda"));
            }

            // ✅ MATCH PAYMENT-BRIVA PATTERN: Update REGISTRATION_STAGES
            // Mark PAYMENT_CICILAN as SELESAI so dashboard detects the change
            log.info("📝 [CICILAN-PAYMENT] Updating REGISTRATION_STAGES for student {}", student.getId());
            
            RegistrationStatus cicilanPaymentStatus = registrationStatusRepository
                    .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_CICILAN_1)
                    .orElse(null);
            
            if (cicilanPaymentStatus != null) {
                cicilanPaymentStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                cicilanPaymentStatus.setUpdatedAt(LocalDateTime.now());
                registrationStatusRepository.save(cicilanPaymentStatus);
                log.info("✅ Registration status PAYMENT_CICILAN_1 updated to SELESAI");
            } else {
                // Create new PAYMENT_CICILAN_1 status if doesn't exist
                cicilanPaymentStatus = new RegistrationStatus();
                cicilanPaymentStatus.setUser(user);
                cicilanPaymentStatus.setStage(RegistrationStatus.RegistrationStage.PAYMENT_CICILAN_1);
                cicilanPaymentStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                cicilanPaymentStatus.setCreatedAt(LocalDateTime.now());
                cicilanPaymentStatus.setUpdatedAt(LocalDateTime.now());
                registrationStatusRepository.save(cicilanPaymentStatus);
                log.info("✅ NEW: Created PAYMENT_CICILAN_1 registration status with SELESAI");
            }

            log.info("✅ Cicilan {} marked as payment-submitted - Dashboard will detect on next polling", id);
            
            CicilanRequestDTO dto = convertToDTO(cicilan);
            return ResponseEntity.ok(new SuccessResponse("Status cicilan diperbarui - menunggu verifikasi admin", dto));

        } catch (Exception e) {
            log.error("❌ Error marking payment as submitted:", e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Utility: Convert entity to DTO
     */
    private CicilanRequestDTO convertToDTO(CicilanRequest cr) {
        return CicilanRequestDTO.builder()
                .id(cr.getId())
                .studentId(cr.getStudent().getId())
                .studentName(cr.getStudent().getFullName())
                .studentEmail(cr.getStudent().getUser().getEmail())
                .programStudiId(cr.getProgramStudi().getId())
                .programStudiName(cr.getProgramStudi().getNama())
                .admissionFormId(cr.getAdmissionForm().getId())
                .jumlahCicilan(cr.getJumlahCicilan())
                .hargaCicilan1(cr.getHargaCicilan1())
                .hargaTotal(cr.getHargaTotal())
                .hargaPerCicilan(cr.getHargaPerCicilan())
                .status(cr.getStatus().name())
                .statusLabel(cr.getStatus().getLabel())
                .catatan(cr.getCatatan())
                .briva(cr.getBriva())
                .paymentMethod(cr.getPaymentMethod().name())
                .paymentMethodLabel(cr.getPaymentMethod().getLabel())
                .approvedBy(cr.getApprovedBy())
                .approvedAt(cr.getApprovedAt())
                .createdAt(cr.getCreatedAt())
                .updatedAt(cr.getUpdatedAt())
                .build();
    }

    // ===== HELPER CLASSES =====
    public static class ErrorResponse {
        public String message;
        public ErrorResponse(String message) { this.message = message; }
    }

    public static class SuccessResponse {
        public String message;
        public Object data;
        public SuccessResponse(String message, Object data) {
            this.message = message;
            this.data = data;
        }
    }
}
