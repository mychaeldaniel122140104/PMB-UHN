package com.uhn.pmb.controller;

import com.uhn.pmb.entity.CicilanRequest;
import com.uhn.pmb.entity.HasilAkhir;
import com.uhn.pmb.dto.CicilanRequestDTO;
import com.uhn.pmb.repository.CicilanRequestRepository;
import com.uhn.pmb.repository.HasilAkhirRepository;
import com.uhn.pmb.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/cicilan")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminCicilanController {

    @Autowired private CicilanRequestRepository cicilanRequestRepository;
    @Autowired private HasilAkhirRepository hasilAkhirRepository;
    @Autowired private EmailService emailService;

    /**
     * Get pending cicilan requests (paginated)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CicilanRequest> requests = cicilanRequestRepository.findPendingRequests(pageable);
            
            var dtos = requests.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PageResponse(dtos, requests.getTotalElements(), requests.getTotalPages()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Approve cicilan request (bisa edit jumlah cicilan & harga & BRIVA)
     * ✅ BRIVA sekarang OPTIONAL - jika tidak diberikan, gunakan nilai yang sudah ada
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveCicilanRequest(
            @PathVariable Long id,
            @RequestBody ApproveCicilanRequest request) {
        try {
            @SuppressWarnings("null")
            CicilanRequest cicilan = cicilanRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cicilan tidak ditemukan"));

            // Validate jumlah cicilan if provided
            if (request.getJumlahCicilan() != null) {
                if (request.getJumlahCicilan() < 1 || request.getJumlahCicilan() > 6) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Jumlah cicilan harus 1-6"));
                }
                cicilan.setJumlahCicilan(request.getJumlahCicilan());
            }

            // Update harga cicilan 1 if provided
            if (request.getHargaCicilan1() != null) {
                cicilan.setHargaCicilan1(request.getHargaCicilan1());
            }

            // Recalculate hargaPerCicilan
            if (cicilan.getJumlahCicilan() > 0) {
                cicilan.setHargaPerCicilan(cicilan.getHargaTotal() / cicilan.getJumlahCicilan());
            }

            // ✅ NEW: BRIVA sekarang optional
            // Jika BRIVA diberikan → update, jika tidak diberikan → gunakan yang sudah ada
            if (request.getBriva() != null && !request.getBriva().trim().isEmpty()) {
                cicilan.setBriva(request.getBriva().trim());
            }
            // Jika BRIVA masih kosong (tidak diberikan dan belum ada) → warning tapi tetap lanjut edit
            // Admin bisa edit jumlah/harga dulu, BRIVA bisa diisi nanti

            // Set approval info
            cicilan.setStatus(CicilanRequest.CicilanRequestStatus.APPROVED);
            cicilan.setApprovedBy(request.getApprovedBy());
            cicilan.setApprovedAt(LocalDateTime.now());

            CicilanRequest saved = cicilanRequestRepository.save(cicilan);
            CicilanRequestDTO dto = convertToDTO(saved);

            // ✅ NEW: Send approval email with cicilan details
            try {
                String studentEmail = saved.getStudent().getUser().getEmail();
                String studentName = saved.getStudent().getFullName();
                String programName = saved.getProgramStudi().getNama();
                
                String emailSubject = "✅ Request Cicilan Diterima - PMB HKBP Nommensen";
                String emailBody = String.format(
                    "<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5; border-radius: 8px;'>" +
                    "<div style='background: #27ae60; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;'>" +
                    "<h2 style='margin: 0;'>✅ Request Cicilan Diterima</h2>" +
                    "</div>" +
                    "<div style='background: white; padding: 30px; border-radius: 0 0 8px 8px;'>" +
                    "<p>Halo <strong>%s</strong>,</p>" +
                    "<p>Selamat! Request cicilan Anda telah <strong>DITERIMA</strong> oleh admin.</p>" +
                    "<div style='background: #f0fdf4; border: 2px solid #27ae60; border-radius: 8px; padding: 20px; margin: 20px 0;'>" +
                    "<h3 style='color: #27ae60; margin-top: 0;'>📋 Detail Cicilan Anda</h3>" +
                    "<table style='width: 100%%; border-collapse: collapse;'>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Program Studi</strong></td><td style='padding: 10px; text-align: right;'><strong>%s</strong></td></tr>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Jumlah Cicilan</strong></td><td style='padding: 10px; text-align: right;'><strong>%d x</strong></td></tr>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Cicilan 1 (DP)</strong></td><td style='padding: 10px; text-align: right;'><strong>Rp %s</strong></td></tr>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Harga Per Cicilan (2-x)</strong></td><td style='padding: 10px; text-align: right;'><strong>Rp %s</strong></td></tr>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Total Harga</strong></td><td style='padding: 10px; text-align: right;'><strong style='color: #27ae60; font-size: 18px;'>Rp %s</strong></td></tr>" +
                    "<tr><td style='padding: 10px; color: #666;'><strong>🏦 BRIVA Anda</strong></td><td style='padding: 10px; text-align: right;'><strong style='font-size: 16px; color: #e74c3c;'>%s</strong></td></tr>" +
                    "</table></div>" +
                    "</div></div></body></html>",
                    studentName, programName, saved.getJumlahCicilan(),
                    formatCurrency(saved.getHargaCicilan1()),
                    formatCurrency(saved.getHargaPerCicilan()),
                    formatCurrency(saved.getHargaTotal()),
                    saved.getBriva()
                );
                
                emailService.sendHtmlEmail(studentEmail, emailSubject, emailBody);
            } catch (Exception emailErr) {
                System.err.println("❌ Email send failed: " + emailErr.getMessage());
            }

            return ResponseEntity.ok(new SuccessResponse("Cicilan request disetujui", dto));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    // ===== NEW: Endpoint untuk frontend (dengan routing /admin/api/installment-request/) =====
    
    /**
     * Approve installment request - NEW endpoint untuk frontend
     */
    @PutMapping(value = "/{id}/approve", produces = "application/json")
    @PostMapping("/approve-installment")
    public ResponseEntity<?> approveInstallmentFromFrontend(
            @PathVariable(required = false) Long id,
            @RequestParam(required = false) Long installmentId) {
        try {
            Long finalId = (id != null) ? id : installmentId;
            if (finalId == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID cicilan harus diberikan"));
            }

            CicilanRequest cicilan = cicilanRequestRepository.findById(finalId)
                    .orElseThrow(() -> new RuntimeException("Cicilan tidak ditemukan"));

            // Set approval status
            cicilan.setStatus(CicilanRequest.CicilanRequestStatus.APPROVED);
            cicilan.setApprovedAt(LocalDateTime.now());

            CicilanRequest saved = cicilanRequestRepository.save(cicilan);
            CicilanRequestDTO dto = convertToDTO(saved);

            // ✅ COPY BRIVA TO HASIL_AKHIR TABLE
            try {
                var student = saved.getStudent();
                if (student != null && saved.getBriva() != null) {
                    // Find or create HasilAkhir record for this student
                    var hasilAkhirOpt = hasilAkhirRepository.findByStudent(student);
                    HasilAkhir hasilAkhir;
                    
                    if (hasilAkhirOpt.isPresent()) {
                        // Update existing record
                        hasilAkhir = hasilAkhirOpt.get();
                        System.out.println("📌 [BRIVA-COPY] Updating existing HASIL_AKHIR record for student: " + student.getId());
                    } else {
                        // Create new record if doesn't exist
                        hasilAkhir = new HasilAkhir();
                        hasilAkhir.setStudent(student);
                        hasilAkhir.setUser(student.getUser());
                        System.out.println("📌 [BRIVA-COPY] Creating new HASIL_AKHIR record for student: " + student.getId());
                    }
                    
                    // Copy BRIVA from cicilan to hasil_akhir
                    hasilAkhir.setBrivaNumber(saved.getBriva());
                    hasilAkhir.setBrivaAmount(new BigDecimal(saved.getHargaTotal()));
                    hasilAkhir.setStatus(HasilAkhir.HasilAkhirStatus.ACTIVE);
                    hasilAkhir.setUpdatedAt(LocalDateTime.now());
                    
                    hasilAkhirRepository.save(hasilAkhir);
                    System.out.println("✅ [BRIVA-COPY] BRIVA '" + saved.getBriva() + "' copied to HASIL_AKHIR for student: " + student.getId());
                }
            } catch (Exception brivaErr) {
                System.err.println("⚠️ [BRIVA-COPY] Failed to copy BRIVA to HASIL_AKHIR: " + brivaErr.getMessage());
                brivaErr.printStackTrace();
            }

            // Send approval email
            try {
                String studentEmail = saved.getStudent().getUser().getEmail();
                String studentName = saved.getStudent().getFullName();
                String programName = saved.getProgramStudi().getNama();
                
                String emailSubject = "✅ Request Cicilan Diterima - PMB HKBP Nommensen";
                String emailBody = String.format(
                    "<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5; border-radius: 8px;'>" +
                    "<div style='background: #27ae60; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;'>" +
                    "<h2 style='margin: 0;'>✅ Request Cicilan Diterima</h2>" +
                    "</div>" +
                    "<div style='background: white; padding: 30px; border-radius: 0 0 8px 8px;'>" +
                    "<p>Halo <strong>%s</strong>,</p>" +
                    "<p>Selamat! Request cicilan Anda telah <strong>DITERIMA</strong> oleh admin.</p>" +
                    "<div style='background: #f0fdf4; border: 2px solid #27ae60; border-radius: 8px; padding: 20px; margin: 20px 0;'>" +
                    "<h3 style='color: #27ae60; margin-top: 0;'>📋 Detail Cicilan Anda</h3>" +
                    "<table style='width: 100%%; border-collapse: collapse;'>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Program Studi</strong></td><td style='padding: 10px; text-align: right;'><strong>%s</strong></td></tr>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Jumlah Cicilan</strong></td><td style='padding: 10px; text-align: right;'><strong>%d x</strong></td></tr>" +
                    "<tr style='border-bottom: 1px solid #ddd;'><td style='padding: 10px; color: #666;'><strong>Total Harga</strong></td><td style='padding: 10px; text-align: right;'><strong style='color: #27ae60; font-size: 18px;'>Rp %s</strong></td></tr>" +
                    "<tr><td style='padding: 10px; color: #666;'><strong>🏦 BRIVA Anda</strong></td><td style='padding: 10px; text-align: right;'><strong style='font-size: 16px; color: #e74c3c;'>%s</strong></td></tr>" +
                    "</table></div>" +
                    "</div></div></body></html>",
                    studentName, programName, saved.getJumlahCicilan(),
                    formatCurrency(saved.getHargaTotal()),
                    saved.getBriva()
                );
                
                emailService.sendHtmlEmail(studentEmail, emailSubject, emailBody);
            } catch (Exception emailErr) {
                System.err.println("❌ Email send failed: " + emailErr.getMessage());
            }

            return ResponseEntity.ok(new SuccessResponse("Cicilan request disetujui", dto));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Reject installment request - NEW endpoint untuk frontend dengan alasan
     */
    @PutMapping(value = "/{id}/reject", produces = "application/json")
    @PostMapping("/reject-installment")
    public ResponseEntity<?> rejectInstallmentFromFrontend(
            @PathVariable(required = false) Long id,
            @RequestParam(required = false) Long installmentId,
            @RequestBody RejectInstallmentRequest request) {
        try {
            // ✅ DEBUG: Log incoming request
            System.out.println("🔍 [BACKEND-DEBUG] rejectInstallmentFromFrontend called");
            System.out.println("   - Path ID: " + id);
            System.out.println("   - Request param installmentId: " + installmentId);
            System.out.println("   - Request object: " + request);
            if (request != null) {
                System.out.println("   - Request.reason: '" + request.getReason() + "'");
                System.out.println("   - Request.reason is null: " + (request.getReason() == null));
                System.out.println("   - Request.reason.trim(): '" + (request.getReason() != null ? request.getReason().trim() : "NULL") + "'");
                System.out.println("   - Request.reason.isEmpty: " + (request.getReason() != null ? request.getReason().isEmpty() : "N/A"));
            }
            
            Long finalId = (id != null) ? id : installmentId;
            if (finalId == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID cicilan harus diberikan"));
            }

            CicilanRequest cicilan = cicilanRequestRepository.findById(finalId)
                    .orElseThrow(() -> new RuntimeException("Cicilan tidak ditemukan"));

            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                System.out.println("❌ [BACKEND-DEBUG] Reason validation FAILED");
                return ResponseEntity.badRequest().body(new ErrorResponse("Alasan penolakan harus diisi"));
            }
            
            System.out.println("✅ [BACKEND-DEBUG] Reason validation PASSED");

            // Set rejection info
            cicilan.setStatus(CicilanRequest.CicilanRequestStatus.REJECTED);
            cicilan.setCatatan(request.getReason());
            cicilan.setApprovedAt(LocalDateTime.now());

            CicilanRequest saved = cicilanRequestRepository.save(cicilan);
            CicilanRequestDTO dto = convertToDTO(saved);

            // Send rejection email
            try {
                String studentEmail = request.getStudentEmail() != null ? request.getStudentEmail() : saved.getStudent().getUser().getEmail();
                String studentName = saved.getStudent().getFullName();
                
                String emailSubject = "❌ Notifikasi: Request Cicilan Ditolak - PMB HKBP Nommensen";
                String emailBody = String.format(
                    "<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5; border-radius: 8px;'>" +
                    "<div style='background: #e74c3c; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;'>" +
                    "<h2 style='margin: 0;'>❌ Request Cicilan Ditolak</h2>" +
                    "</div>" +
                    "<div style='background: white; padding: 30px; border-radius: 0 0 8px 8px;'>" +
                    "<p>Halo <strong>%s</strong>,</p>" +
                    "<p>Maaf, request cicilan Anda telah <strong>DITOLAK</strong> oleh admin.</p>" +
                    "<div style='background: #fef5f5; border: 2px solid #e74c3c; border-radius: 8px; padding: 20px; margin: 20px 0;'>" +
                    "<h3 style='color: #e74c3c; margin-top: 0;'>📋 Alasan Penolakan</h3>" +
                    "<p style='color: #666; white-space: pre-line;'>%s</p>" +
                    "</div>" +
                    "<div style='background: #f0f7ff; border-left: 4px solid #3498db; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<p style='margin: 0; color: #2c3e50;'>" +
                    "<strong>ℹ️ Langkah Selanjutnya:</strong><br>" +
                    "Silahkan perbaiki data sesuai dengan alasan di atas dan lakukan kembali request cicilan. Kami siap membantu jika ada pertanyaan." +
                    "</p></div>" +
                    "</div></div></body></html>",
                    studentName, request.getReason().replace("\n", "<br>")
                );
                
                emailService.sendHtmlEmail(studentEmail, emailSubject, emailBody);
            } catch (Exception emailErr) {
                System.err.println("❌ Email send failed: " + emailErr.getMessage());
            }

            return ResponseEntity.ok(new SuccessResponse("Cicilan request ditolak dan email telah dikirim", dto));

        } catch (Exception e) {
            System.out.println("❌ [BACKEND-DEBUG] Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get all cicilan requests by status
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<?> getByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            CicilanRequest.CicilanRequestStatus statusEnum = CicilanRequest.CicilanRequestStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<CicilanRequest> requests = cicilanRequestRepository.findByStatus(statusEnum, pageable);
            
            var dtos = requests.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PageResponse(dtos, requests.getTotalElements(), requests.getTotalPages()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Delete cicilan request
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCicilanRequest(@PathVariable Long id) {
        try {
            CicilanRequest cicilan = cicilanRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cicilan tidak ditemukan"));

            cicilanRequestRepository.deleteById(id);
            return ResponseEntity.ok(new SuccessResponse("Cicilan request berhasil dihapus", null));

        } catch (Exception e) {
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
                .admissionFormId(cr.getAdmissionForm() != null ? cr.getAdmissionForm().getId() : null)
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
    public static class ApproveCicilanRequest {
        private Integer jumlahCicilan;
        private Long hargaCicilan1;
        private String briva;
        private String approvedBy;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RejectCicilanRequest {
        private String catatan;
        private String rejectBy;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RejectInstallmentRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("reason")
        private String reason;
        
        @com.fasterxml.jackson.annotation.JsonProperty("studentEmail")
        private String studentEmail;
        
        // Explicit setters to ensure deserialization works
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getReason() {
            return this.reason;
        }
        
        public void setStudentEmail(String studentEmail) {
            this.studentEmail = studentEmail;
        }
        
        public String getStudentEmail() {
            return this.studentEmail;
        }
    }

    /**
     * Format currency value to Indonesian format (Rp X.XXX.XXX)
     */
    private String formatCurrency(Long value) {
        if (value == null) return "0";
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0");
        df.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(java.util.Locale.of("id", "ID")));
        return df.format(value);
    }
}
