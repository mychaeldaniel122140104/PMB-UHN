package com.uhn.pmb.controller;

import com.uhn.pmb.entity.CicilanRequest;
import com.uhn.pmb.entity.ManualCicilanPayment;
import com.uhn.pmb.entity.Student;
import com.uhn.pmb.entity.User;
import com.uhn.pmb.dto.ManualCicilanPaymentDTO;
import com.uhn.pmb.repository.CicilanRequestRepository;
import com.uhn.pmb.repository.ManualCicilanPaymentRepository;
import com.uhn.pmb.repository.StudentRepository;
import com.uhn.pmb.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manual-cicilan")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManualCicilanPaymentController {

    @Autowired private ManualCicilanPaymentRepository manualPaymentRepository;
    @Autowired private CicilanRequestRepository cicilanRequestRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;

    // ✅ FIX: Resolve absolute path using user.dir (project root)
    private static String getUploadDir() {
        String baseDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "cicilan-proofs" + File.separator;
        return baseDir;
    }

    /**
     * Submit manual payment proof (Student)
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPaymentProof(
            @RequestParam Long cicilanRequestId,
            @RequestParam Integer cicilanKe,
            @RequestParam Long nominal,
            @RequestParam("paymentProof") MultipartFile file,
            Authentication auth) {
        try {
            // ✅ FIX: Safely extract User from authentication
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
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student tidak ditemukan"));

            // Get cicilan request
            CicilanRequest cicilan = cicilanRequestRepository.findById(cicilanRequestId)
                    .orElseThrow(() -> new RuntimeException("Cicilan request tidak ditemukan"));

            // Verify ownership
            if (!cicilan.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Cicilan bukan milik Anda"));
            }

            // Verify cicilan is approved
            if (!cicilan.getStatus().equals(CicilanRequest.CicilanRequestStatus.APPROVED)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Cicilan harus disetujui terlebih dahulu"));
            }

            // Verify payment method is MANUAL
            if (!cicilan.getPaymentMethod().equals(CicilanRequest.PaymentMethod.MANUAL)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Metode pembayaran harus MANUAL"));
            }

            // Check if payment already exists for this cicilan ke
            if (manualPaymentRepository.findByCicilanRequestIdAndCicilanKe(cicilanRequestId, cicilanKe).isPresent()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Pembayaran cicilan " + cicilanKe + " sudah pernah disubmit"));
            }

            // ✅ Save file with absolute path
            String uploadPath = getUploadDir() + "student_" + student.getId() + "_cicilan_" + cicilanRequestId + "_ke_" + cicilanKe + File.separator;
            
            try {
                Files.createDirectories(Paths.get(uploadPath));
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Gagal membuat direktori upload: " + e.getMessage()));
            }

            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.contains(".")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File tidak valid"));
            }
            
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = "proof_" + System.currentTimeMillis() + fileExtension;
            String filePath = uploadPath + fileName;

            try {
                file.transferTo(new File(filePath));
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Gagal upload file: " + e.getMessage()));
            }

            // Create manual payment record
            ManualCicilanPayment payment = ManualCicilanPayment.builder()
                    .cicilanRequest(cicilan)
                    .student(student)
                    .cicilanKe(cicilanKe)
                    .nominal(nominal)
                    .paymentProofPath(filePath)
                    .status(ManualCicilanPayment.ManualPaymentStatus.PENDING)
                    .build();

            ManualCicilanPayment saved = manualPaymentRepository.save(payment);
            ManualCicilanPaymentDTO dto = convertToDTO(saved);

            return ResponseEntity.ok(new SuccessResponse("Bukti pembayaran berhasil disubmit. Menunggu verifikasi admin.", dto));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get payment status for cicilan (Student)
     */
    @GetMapping("/status/{cicilanRequestId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable Long cicilanRequestId,
            Authentication auth) {
        try {
            // ✅ FIX: Safely extract User from authentication
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
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student tidak ditemukan"));

            CicilanRequest cicilan = cicilanRequestRepository.findById(cicilanRequestId)
                    .orElseThrow(() -> new RuntimeException("Cicilan request tidak ditemukan"));

            if (!cicilan.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Cicilan bukan milik Anda"));
            }

            List<ManualCicilanPayment> payments = manualPaymentRepository.findByStudentAndCicilanRequest(student.getId(), cicilanRequestId);
            List<ManualCicilanPaymentDTO> dtos = payments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get pending manual payments for admin verification (Paginated)
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ManualCicilanPayment> payments = manualPaymentRepository.findPendingPayments(pageable);

            var dtos = payments.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PageResponse(dtos, payments.getTotalElements(), payments.getTotalPages()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Admin verify manual payment
     */
    @PutMapping("/admin/{id}/verify")
    public ResponseEntity<?> verifyPayment(
            @PathVariable Long id,
            @RequestBody VerifyPaymentRequest request) {
        try {
            ManualCicilanPayment payment = manualPaymentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pembayaran tidak ditemukan"));

            if (!payment.getStatus().equals(ManualCicilanPayment.ManualPaymentStatus.PENDING)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Pembayaran sudah diverifikasi/ditolak"));
            }

            payment.setStatus(ManualCicilanPayment.ManualPaymentStatus.VERIFIED);
            payment.setVerifiedBy(request.getVerifiedBy());
            payment.setVerifiedAt(LocalDateTime.now());
            payment.setKeterangan(request.getKeterangan());

            ManualCicilanPayment saved = manualPaymentRepository.save(payment);
            ManualCicilanPaymentDTO dto = convertToDTO(saved);

            return ResponseEntity.ok(new SuccessResponse("Pembayaran diverifikasi", dto));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Admin reject manual payment
     */
    @PutMapping("/admin/{id}/reject")
    public ResponseEntity<?> rejectPayment(
            @PathVariable Long id,
            @RequestBody RejectPaymentRequest request) {
        try {
            ManualCicilanPayment payment = manualPaymentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pembayaran tidak ditemukan"));

            if (!payment.getStatus().equals(ManualCicilanPayment.ManualPaymentStatus.PENDING)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Pembayaran sudah diverifikasi/ditolak"));
            }

            if (request.getAlasan() == null || request.getAlasan().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Alasan penolakan harus diisi"));
            }

            payment.setStatus(ManualCicilanPayment.ManualPaymentStatus.REJECTED);
            payment.setVerifiedBy(request.getRejectedBy());
            payment.setVerifiedAt(LocalDateTime.now());
            payment.setKeterangan(request.getAlasan());

            ManualCicilanPayment saved = manualPaymentRepository.save(payment);
            ManualCicilanPaymentDTO dto = convertToDTO(saved);

            return ResponseEntity.ok(new SuccessResponse("Pembayaran ditolak", dto));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Utility: Convert entity to DTO
     */
    private ManualCicilanPaymentDTO convertToDTO(ManualCicilanPayment payment) {
        return ManualCicilanPaymentDTO.builder()
                .id(payment.getId())
                .cicilanRequestId(payment.getCicilanRequest().getId())
                .studentId(payment.getStudent().getId())
                .studentName(payment.getStudent().getFullName())
                .studentEmail(payment.getStudent().getUser().getEmail())
                .cicilanKe(payment.getCicilanKe())
                .nominal(payment.getNominal())
                .paymentProofPath(payment.getPaymentProofPath())
                .status(payment.getStatus().name())
                .statusLabel(payment.getStatus().getLabel())
                .keterangan(payment.getKeterangan())
                .verifiedBy(payment.getVerifiedBy())
                .verifiedAt(payment.getVerifiedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
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

    public static class PageResponse {
        public Object content;
        public long totalElements;
        public int totalPages;
        public PageResponse(Object content, long totalElements, int totalPages) {
            this.content = content;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VerifyPaymentRequest {
        private String verifiedBy;
        private String keterangan; // Optional notes
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RejectPaymentRequest {
        private String alasan;
        private String rejectedBy;
    }
}
