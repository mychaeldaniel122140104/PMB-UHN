package com.uhn.pmb.controller;

import com.uhn.pmb.dto.UjianLinkRequest;
import com.uhn.pmb.entity.GelombangLinkUjian;
import com.uhn.pmb.entity.RegistrationPeriod;
import com.uhn.pmb.repository.GelombangLinkUjianRepository;
import com.uhn.pmb.repository.RegistrationPeriodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/admin/api/ujian-links")
@CrossOrigin(origins = "*")
public class AdminUjianLinkController {

    @Autowired
    private GelombangLinkUjianRepository ujianLinkRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    /**
     * Get all ujian links
     * GET /admin/api/ujian-links
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllUjianLinks(Authentication authentication) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to getAllUjianLinks");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            List<GelombangLinkUjian> links = ujianLinkRepository.findAllByOrderByUpdatedAtDesc();
            
            log.info("✅ Retrieved {} ujian links", links.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", links,
                "total", links.size()
            ));
        } catch (Exception e) {
            log.error("❌ Error retrieving ujian links", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Get ujian link by period ID
     * GET /admin/api/ujian-links/by-period/{periodId}
     * ✅ UPDATED: Allow CAMABA to fetch ujian link for their exam
     */
    @GetMapping("/by-period/{periodId}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getUjianLinkByPeriod(
            @PathVariable Long periodId,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to getUjianLinkByPeriod");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            Optional<GelombangLinkUjian> link = ujianLinkRepository.findByRegistrationPeriodId(periodId);

            if (link.isEmpty()) {
                log.warn("⚠️ Ujian link not found for period ID: {}", periodId);
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Ujian link not found for this period"
                ));
            }

            log.info("✅ Retrieved ujian link for period ID: {}", periodId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", link.get()
            ));
        } catch (Exception e) {
            log.error("❌ Error retrieving ujian link for period {}", periodId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Save new ujian link (Online or Offline)
     * POST /admin/api/ujian-links
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> saveUjianLink(
            @RequestBody UjianLinkRequest request,
            Authentication authentication) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to saveUjianLink");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            // Validate request
            if (request.getPeriodId() == null) {
                log.warn("❌ Invalid request: missing periodId");
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "Period ID is required"
                ));
            }

            // Check if period exists
            Optional<RegistrationPeriod> period = registrationPeriodRepository.findById(request.getPeriodId());
            if (period.isEmpty()) {
                log.warn("❌ Period not found: {}", request.getPeriodId());
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Registration period not found"
                ));
            }

            // Check if ujian link already exists for this period (1-to-1 relationship)
            Optional<GelombangLinkUjian> existing = ujianLinkRepository.findByRegistrationPeriodId(request.getPeriodId());
            if (existing.isPresent()) {
                log.warn("⚠️ Ujian link already exists for period: {}", request.getPeriodId());
                return ResponseEntity.status(409).body(Map.of(
                    "success", false,
                    "message", "Ujian link already exists for this period"
                ));
            }

            // Create and save new ujian link (either online or offline)
            GelombangLinkUjian ujianLink = GelombangLinkUjian.builder()
                    .registrationPeriod(period.get())
                    .linkUjian(request.getLinkUjian())
                    .examDate(request.getExamDate())
                    .examPlace(request.getExamPlace())
                    .examTime(request.getExamTime())
                    .build();

            GelombangLinkUjian saved = ujianLinkRepository.save(ujianLink);
            
            log.info("✅ Ujian link saved successfully for period: {}", request.getPeriodId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ujian link saved successfully",
                "data", saved
            ));
        } catch (Exception e) {
            log.error("❌ Error saving ujian link", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Update ujian link
     * PUT /admin/api/ujian-links
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> updateUjianLink(
            @RequestBody UjianLinkRequest request,
            Authentication authentication) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to updateUjianLink");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            // Validate request
            if (request.getPeriodId() == null || request.getLinkUjian() == null) {
                log.warn("❌ Invalid request: missing periodId or linkUjian");
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "Period ID and link ujian are required"
                ));
            }

            // Find existing ujian link
            Optional<GelombangLinkUjian> existing = ujianLinkRepository.findByRegistrationPeriodId(request.getPeriodId());
            if (existing.isEmpty()) {
                log.warn("❌ Ujian link not found for period: {}", request.getPeriodId());
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Ujian link not found for this period"
                ));
            }

            // Update the ujian link
            GelombangLinkUjian ujianLink = existing.get();
            ujianLink.setLinkUjian(request.getLinkUjian());
            
            GelombangLinkUjian updated = ujianLinkRepository.save(ujianLink);
            
            log.info("✅ Ujian link updated successfully for period: {}", request.getPeriodId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ujian link updated successfully",
                "data", updated
            ));
        } catch (Exception e) {
            log.error("❌ Error updating ujian link", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete ujian link
     * DELETE /admin/api/ujian-links/{periodId}
     */
    @DeleteMapping("/{periodId}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> deleteUjianLink(
            @PathVariable Long periodId,
            Authentication authentication) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to deleteUjianLink");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            // Check if ujian link exists
            Optional<GelombangLinkUjian> existing = ujianLinkRepository.findByRegistrationPeriodId(periodId);
            if (existing.isEmpty()) {
                log.warn("❌ Ujian link not found for period: {}", periodId);
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Ujian link not found"
                ));
            }

            // Delete the ujian link
            ujianLinkRepository.deleteByRegistrationPeriodId(periodId);
            
            log.info("✅ Ujian link deleted successfully for period: {}", periodId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ujian link deleted successfully"
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting ujian link", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Save offline exam details
     * POST /admin/api/offline-exams
     */
    @PostMapping("/offline-exams")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> saveOfflineExam(
            @RequestBody UjianLinkRequest request,
            Authentication authentication) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to saveOfflineExam");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            // Validate request
            if (request.getPeriodId() == null || request.getExamDate() == null || 
                request.getExamPlace() == null || request.getExamTime() == null) {
                log.warn("❌ Invalid request: missing required fields");
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "Period ID, exam date, place, and time are required"
                ));
            }

            // Check if period exists
            Optional<RegistrationPeriod> period = registrationPeriodRepository.findById(request.getPeriodId());
            if (period.isEmpty()) {
                log.warn("❌ Period not found: {}", request.getPeriodId());
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Registration period not found"
                ));
            }

            // Check if exam already exists for this period
            Optional<GelombangLinkUjian> existing = ujianLinkRepository.findByRegistrationPeriodId(request.getPeriodId());
            if (existing.isPresent()) {
                log.warn("⚠️ Exam already exists for period: {}", request.getPeriodId());
                return ResponseEntity.status(409).body(Map.of(
                    "success", false,
                    "message", "Exam already exists for this period"
                ));
            }

            // Create and save offline exam
            GelombangLinkUjian offlineExam = GelombangLinkUjian.builder()
                    .registrationPeriod(period.get())
                    .examDate(request.getExamDate())
                    .examPlace(request.getExamPlace())
                    .examTime(request.getExamTime())
                    .build();

            GelombangLinkUjian saved = ujianLinkRepository.save(offlineExam);
            
            log.info("✅ Offline exam saved successfully for period: {}", request.getPeriodId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Offline exam saved successfully",
                "data", saved
            ));
        } catch (Exception e) {
            log.error("❌ Error saving offline exam", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete offline exam
     * DELETE /admin/api/offline-exams/{periodId}
     */
    @DeleteMapping("/offline-exams/{periodId}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> deleteOfflineExam(
            @PathVariable Long periodId,
            Authentication authentication) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthorized access to deleteOfflineExam");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Unauthorized"
                ));
            }

            // Check if exam exists
            Optional<GelombangLinkUjian> existing = ujianLinkRepository.findByRegistrationPeriodId(periodId);
            if (existing.isEmpty()) {
                log.warn("❌ Offline exam not found for period: {}", periodId);
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Offline exam not found"
                ));
            }

            // Delete the offline exam
            ujianLinkRepository.deleteByRegistrationPeriodId(periodId);
            
            log.info("✅ Offline exam deleted successfully for period: {}", periodId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Offline exam deleted successfully"
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting offline exam", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
}
