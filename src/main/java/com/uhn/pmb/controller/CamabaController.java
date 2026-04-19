package com.uhn.pmb.controller;

import com.uhn.pmb.dto.StudentProfileRequest;
import com.uhn.pmb.dto.ChangePasswordRequest;
import com.uhn.pmb.dto.AdmissionFormSubmitRequest;
import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import com.uhn.pmb.service.StudentRegistrationService;
import com.uhn.pmb.service.RegistrationStatusService;
import com.uhn.pmb.service.EmailService;
import com.uhn.pmb.service.BrivaService;
import com.uhn.pmb.service.ExamTokenService;
import com.uhn.pmb.service.ValidationStatusTrackerService;
import com.uhn.pmb.service.HasilAkhirService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camaba")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('CAMABA')")
public class CamabaController {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final SelectionTypeRepository selectionTypeRepository;
    private final JenisSeleksiRepository jenisSeleksiRepository;
    private final PeriodJenisSeleksiRepository periodJenisSeleksiRepository;
    private final SelectionProgramStudiRepository selectionProgramStudiRepository;
    private final StudentRegistrationService registrationService;
    private final BrivaService brivaService;
    private final EmailService emailService;
    private final ExamTokenService examTokenService;
    private final AdmissionFormRepository admissionFormRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final FormValidationRepository formValidationRepository;
    private final RegistrationStatusRepository registrationStatusRepository;
    private final RegistrationStatusService registrationStatusService;
    private final AdminMessageRepository adminMessageRepository;
    private final ExamTokenRepository tokenRepository;
    // ✅ NEW: Re-enrollment repositories
    private final ReEnrollmentRepository reenrollmentRepository;
    private final ReEnrollmentDocumentRepository reenrollmentDocumentRepository;
    // ✅ NEW: ValidationStatusTrackerService for handling revision submissions
    private final ValidationStatusTrackerService validationStatusTrackerService;
    // ✅ NEW: HasilAkhirService for retrieving final results
    private final HasilAkhirService hasilAkhirService;
    // ✅ NEW: FormRepairStatusRepository for separate repair status tracking
    private final FormRepairStatusRepository formRepairStatusRepository;

    /**
     * Get student profile with user email included
     * FIX: Return object containing both user and student data so frontend can access user.email
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            // Create response object with both user and student data
            Map<String, Object> response = new HashMap<>();
            response.put("id", student.getId());
            response.put("fullName", student.getFullName());
            response.put("nik", student.getNik());
            response.put("birthDate", student.getBirthDate());
            response.put("birthPlace", student.getBirthPlace());
            response.put("gender", student.getGender());
            response.put("address", student.getAddress());
            response.put("phoneNumber", student.getPhoneNumber());
            response.put("email", student.getUser() != null ? student.getUser().getEmail() : "");
            response.put("parentName", student.getParentName());
            response.put("parentPhone", student.getParentPhone());
            response.put("schoolOrigin", student.getSchoolOrigin());
            response.put("schoolYear", student.getSchoolYear());
            response.put("createdAt", student.getCreatedAt());
            response.put("updatedAt", student.getUpdatedAt());
            
            // Add user data including email from authentication
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole());
            userData.put("createdAt", user.getCreatedAt());
            response.put("user", userData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Update student profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody StudentProfileRequest request) {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            student.setFullName(request.getFullName());
            student.setNik(request.getNik());
            student.setBirthDate(request.getBirthDate());
            student.setBirthPlace(request.getBirthPlace());
            student.setGender(request.getGender());
            student.setAddress(request.getAddress());
            student.setPhoneNumber(request.getPhoneNumber());
            student.setParentName(request.getParentName());
            student.setParentPhone(request.getParentPhone());
            student.setSchoolOrigin(request.getSchoolOrigin());
            student.setSchoolYear(request.getSchoolYear());
            
            studentRepository.save(student);
            return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully"));
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Select gelombang pendaftaran
     */
    @PostMapping("/select-gelombang")
    public ResponseEntity<?> selectGelombang(@RequestBody Map<String, Long> request) {
        try {
            Long gelombangId = request.get("gelombangId");
            
            if (gelombangId == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Gelombang ID is required"));
            }

            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // Store selected gelombang in session/localStorage (will be used in next step)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Gelombang berhasil dipilih");
            response.put("gelombangId", gelombangId);
            
            log.info("Student {} selected gelombang {}", email, gelombangId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error selecting gelombang: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get open registration periods
     */
    @GetMapping("/registration-periods")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getOpenRegistrationPeriods() {
        try {
            List<RegistrationPeriod> periods = registrationPeriodRepository
                    .findByStatusOrderByRegStartDateDesc(RegistrationPeriod.Status.OPEN);
            return ResponseEntity.ok(periods);
        } catch (Exception e) {
            log.error("Error fetching periods: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get single registration period by ID
     * GET /api/camaba/registration-periods/{id}
     */
    @GetMapping("/registration-periods/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getRegistrationPeriodById(@PathVariable Long id) {
        try {
            return registrationPeriodRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching period {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all registration periods (for gelombang selection page)
     * Includes OPEN, CLOSED, and displays upcoming periods based on dates
     */
    @GetMapping("/all-gelombang")
    @PreAuthorize("permitAll()")  // ✅ FIXED: Allow public access to gelombang list
    public ResponseEntity<?> getAllGelombang() {
        try {
            List<RegistrationPeriod> periods = registrationPeriodRepository.findAll();
            // Sort by regStartDate ascending (earliest first)
            periods.sort((p1, p2) -> p1.getRegStartDate().compareTo(p2.getRegStartDate()));
            
            List<Map<String, Object>> result = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (RegistrationPeriod period : periods) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", period.getId());
                data.put("name", period.getName());
                data.put("description", period.getDescription());
                data.put("regStartDate", period.getRegStartDate());
                data.put("regEndDate", period.getRegEndDate());
                data.put("examDate", period.getExamDate());
                data.put("examEndDate", period.getExamEndDate());
                data.put("announcementDate", period.getAnnouncementDate());
                data.put("reenrollmentStartDate", period.getReenrollmentStartDate());
                data.put("reenrollmentEndDate", period.getReenrollmentEndDate());
                data.put("status", period.getStatus().toString());
                
                // Calculate effective status for frontend
                String displayStatus = "open";
                if (now.isBefore(period.getRegStartDate())) {
                    displayStatus = "notopen";
                } else if (now.isAfter(period.getRegEndDate()) || period.getStatus() == RegistrationPeriod.Status.CLOSED) {
                    displayStatus = "closed";
                }
                data.put("displayStatus", displayStatus);
                data.put("waveType", period.getWaveType() != null ? period.getWaveType().toString() : "REGULAR_TEST");
                
                result.add(data);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching all gelombang: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get formula selections (Jenis Seleksi) - optionally filter by period (gelombang)
     * REAL-TIME: When periodId provided, returns ONLY JenisSeleksi linked to that period
     * 
     * @param periodId Optional: Filter jenis seleksi for specific gelombang/period
     * @return Active jenis seleksi options available for user selection
     */
    @GetMapping("/all-formulas")
    @PreAuthorize("permitAll()")  // Public: Display available formulas/jenis seleksi
    public ResponseEntity<?> getAllFormulas(
            @RequestParam(value = "periodId", required = false) Long periodId) {
        try {
            List<JenisSeleksi> jenisSeleksiList;
            
            if (periodId != null && periodId > 0) {
                // ✅ REAL-TIME FILTERING: Get JenisSeleksi linked to specific period via PERIOD_JENIS_SELEKSI
                log.info("📋 [FORMULA-REAL-TIME] Fetching jenis seleksi for period ID: {}", periodId);
                
                // Verify period exists
                var period = registrationPeriodRepository.findById(periodId);
                if (period.isEmpty()) {
                    log.warn("⚠️ Period {} not found", periodId);
                    return ResponseEntity.ok(new ArrayList<>());
                }
                
                // ✅ Query M2M junction table: PERIOD_JENIS_SELEKSI to get linked JenisSeleksi
                List<PeriodJenisSeleksi> periodJenisSeleksiList = 
                    periodJenisSeleksiRepository.findByPeriod_IdAndIsActiveTrue(periodId);
                
                // Extract JenisSeleksi objects from junction table
                jenisSeleksiList = new ArrayList<>();
                for (PeriodJenisSeleksi pjs : periodJenisSeleksiList) {
                    jenisSeleksiList.add(pjs.getJenisSeleksi());
                }
                
                log.info("✅ [FORMULA-REAL-TIME] Found {} jenis seleksi for period {}", jenisSeleksiList.size(), periodId);
                
                if (jenisSeleksiList.isEmpty()) {
                    log.warn("⚠️ [FORMULA-REAL-TIME] Period {} has no jenis seleksi linked. Check PERIOD_JENIS_SELEKSI table.", periodId);
                }
            } else {
                // No period ID provided - return all active jenis seleksi
                log.info("📋 [FORMULA-GLOBAL] Fetching all jenis seleksi (no period filter)");
                jenisSeleksiList = jenisSeleksiRepository.findByIsActiveTrueOrderBySortOrder();
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (JenisSeleksi jenis : jenisSeleksiList) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", jenis.getId());
                data.put("code", jenis.getCode());
                data.put("title", jenis.getNama());
                data.put("description", jenis.getDeskripsi());
                data.put("iconEmoji", jenis.getLogoUrl());
                data.put("price", jenis.getHarga());
                data.put("features", jenis.getFasilitas() != null ? 
                    Arrays.asList(jenis.getFasilitas().split(",")) : new ArrayList<>());
                data.put("formType", jenis.getCode());
                data.put("isActive", true);
                
                result.add(data);
            }
            
            log.info("✅ Returning {} active jenis seleksi", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error fetching formulas: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching formulas: " + e.getMessage()));
        }
    }

    /**
     * Get program studi connected to a jenis seleksi
     * ✅ Used by formula-selection.html to display view programs button
     */
    @GetMapping("/jenis-seleksi/{jenisSeleksiId}/program-studi")
    @PreAuthorize("permitAll()")  // Public: Display connected program studi
    public ResponseEntity<?> getProgramStudiByJenisSeleksi(@PathVariable Long jenisSeleksiId) {
        try {
            log.info("📋 [PROGRAM-STUDI-VIEW] Fetching program studi for jenis seleksi ID: {}", jenisSeleksiId);
            
            // Verify jenis seleksi exists
            var jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiId);
            if (jenisSeleksi.isEmpty()) {
                log.warn("⚠️ Jenis seleksi {} not found", jenisSeleksiId);
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            // Query junction table for connected program studi
            List<SelectionProgramStudi> connections = 
                selectionProgramStudiRepository.findByJenisSeleksi_IdAndIsActiveTrue(jenisSeleksiId);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (SelectionProgramStudi conn : connections) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", conn.getProgramStudi().getId());
                data.put("nama", conn.getProgramStudi().getNama());
                data.put("kode", conn.getProgramStudi().getKode());
                data.put("tipe", conn.getProgramStudi().getIsMedical() ? "Kedokteran" : "Non-Kedokteran");
                data.put("deskripsi", conn.getProgramStudi().getDeskripsi() != null ? 
                    conn.getProgramStudi().getDeskripsi() : "");
                
                // ✅ ADD: Price fields for cicilan calculation
                data.put("hargaTotalPerTahun", conn.getProgramStudi().getHargaTotalPerTahun() != null ? 
                    conn.getProgramStudi().getHargaTotalPerTahun() : 0);
                data.put("cicilan1", conn.getProgramStudi().getCicilan1() != null ? 
                    conn.getProgramStudi().getCicilan1() : 0);
                data.put("cicilan2", conn.getProgramStudi().getCicilan2() != null ? 
                    conn.getProgramStudi().getCicilan2() : 0);
                data.put("cicilan3", conn.getProgramStudi().getCicilan3() != null ? 
                    conn.getProgramStudi().getCicilan3() : 0);
                data.put("cicilan4", conn.getProgramStudi().getCicilan4() != null ? 
                    conn.getProgramStudi().getCicilan4() : 0);
                data.put("cicilan5", conn.getProgramStudi().getCicilan5() != null ? 
                    conn.getProgramStudi().getCicilan5() : 0);
                data.put("cicilan6", conn.getProgramStudi().getCicilan6() != null ? 
                    conn.getProgramStudi().getCicilan6() : 0);
                
                result.add(data);
            }
            
            log.info("✅ [PROGRAM-STUDI-VIEW] Found {} program studi for jenis seleksi {}", result.size(), jenisSeleksiId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ Error fetching program studi: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error fetching program studi: " + e.getMessage()));
        }
    }

    /**
     * Get selection types for a period
     */
    @GetMapping("/selection-types/{periodId}")
    @PreAuthorize("permitAll()")  // Public: Display selection types for period
    public ResponseEntity<?> getSelectionTypes(@PathVariable Long periodId) {
        try {
            List<SelectionType> types = selectionTypeRepository
                    .findByPeriod_IdAndIsActiveTrue(periodId);
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error fetching selection types: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get selection type details (including requireTesting flag)
     */
    @GetMapping("/selection-types-detail/{selectionTypeId}")
    @PreAuthorize("permitAll()")  // Public: Display selection type details
    public ResponseEntity<?> getSelectionTypeDetail(@PathVariable Long selectionTypeId) {
        try {
            log.info("📋 Fetching selection type details for ID: {}", selectionTypeId);
            
            SelectionType selectionType = selectionTypeRepository.findById(selectionTypeId)
                    .orElseThrow(() -> new RuntimeException("Selection type with ID " + selectionTypeId + " not found in database"));
            
            log.info("✅ Selection type found: {}", selectionType.getName());
            log.info("   - Price: {}", selectionType.getPrice());
            log.info("   - FormType: {}", selectionType.getFormType());
            log.info("   - RequireTesting: {}", selectionType.getRequireTesting());
            log.info("   - RequireRanking: {}", selectionType.getRequireRanking());
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", selectionType.getId());
            response.put("name", selectionType.getName() != null ? selectionType.getName() : "");
            response.put("requireTesting", selectionType.getRequireTesting() != null ? selectionType.getRequireTesting() : false);
            response.put("requireRanking", selectionType.getRequireRanking() != null ? selectionType.getRequireRanking() : false);
            response.put("description", selectionType.getDescription() != null ? selectionType.getDescription() : "");
            response.put("formType", selectionType.getFormType() != null ? selectionType.getFormType() : "");
            response.put("price", selectionType.getPrice() != null ? selectionType.getPrice() : 0L);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching selection type detail: {}", e.getMessage(), e);
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "Selection type not found: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW ENDPOINT: Get JenisSeleksi detail by ID (corrected mapping to jenis_seleksi table)
     * Used by payment-briva.html instead of selection-types-detail
     */
    @GetMapping("/jenis-seleksi-detail/{jenisSeleksiId}")
    @PreAuthorize("permitAll()")  // Public: Display jenis seleksi details
    public ResponseEntity<?> getJenisSeleksiDetail(@PathVariable Long jenisSeleksiId) {
        try {
            log.info("📋 Fetching jenis seleksi details for ID: {}", jenisSeleksiId);
            
            JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiId)
                    .orElseThrow(() -> new RuntimeException("Jenis seleksi with ID " + jenisSeleksiId + " not found in database"));
            
            log.info("✅ Jenis seleksi found: {}", jenisSeleksi.getNama());
            log.info("   - Harga: {}", jenisSeleksi.getHarga());
            log.info("   - Deskripsi: {}", jenisSeleksi.getDeskripsi());
            log.info("   - Is Active: {}", jenisSeleksi.getIsActive());
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", jenisSeleksi.getId());
            response.put("code", jenisSeleksi.getCode() != null ? jenisSeleksi.getCode() : "");
            response.put("nama", jenisSeleksi.getNama() != null ? jenisSeleksi.getNama() : "");
            response.put("deskripsi", jenisSeleksi.getDeskripsi() != null ? jenisSeleksi.getDeskripsi() : "");
            response.put("fasilitas", jenisSeleksi.getFasilitas() != null ? jenisSeleksi.getFasilitas() : "");
            response.put("harga", jenisSeleksi.getHarga() != null ? jenisSeleksi.getHarga() : 0L);
            response.put("logoUrl", jenisSeleksi.getLogoUrl() != null ? jenisSeleksi.getLogoUrl() : "");
            response.put("isActive", jenisSeleksi.getIsActive() != null ? jenisSeleksi.getIsActive() : true);
            
            // For compatibility with frontend expecting "price" field
            response.put("price", jenisSeleksi.getHarga() != null ? jenisSeleksi.getHarga() : 0L);
            response.put("requireTesting", true); // Default - JenisSeleksi is always test-based
            response.put("requireRanking", false); // Default - depends on wave type
            response.put("formType", "BOTH"); // JenisSeleksi doesn't have formType - it's independent
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching jenis seleksi detail: {}", e.getMessage(), e);
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "Jenis seleksi not found: " + e.getMessage()));
        }
    }

    /**
     * Get JenisSeleksi by ID (alias for jenis-seleksi-detail, used by dashboard-camaba)
     * GET /api/camaba/jenis-seleksi/{id}
     */
    @GetMapping("/jenis-seleksi/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getJenisSeleksiById(@PathVariable Long id) {
        try {
            JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Jenis seleksi " + id + " not found"));
            Map<String, Object> response = new HashMap<>();
            response.put("id", jenisSeleksi.getId());
            response.put("code", jenisSeleksi.getCode());
            response.put("nama", jenisSeleksi.getNama());
            response.put("title", jenisSeleksi.getNama());
            response.put("deskripsi", jenisSeleksi.getDeskripsi());
            response.put("harga", jenisSeleksi.getHarga());
            response.put("price", jenisSeleksi.getHarga());
            response.put("logoUrl", jenisSeleksi.getLogoUrl());
            response.put("isActive", jenisSeleksi.getIsActive());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching jenis seleksi {}: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Select formula (Kedok/Non-Kedok)
     */
    @PostMapping("/select-formula")
    public ResponseEntity<?> selectFormula(@RequestBody Map<String, String> request) {
        try {
            String formula = request.get("formula");
            
            if (formula == null || formula.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Formula is required"));
            }

            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Formula " + formula + " berhasil dipilih");
            response.put("formula", formula);
            response.put("price", formula.equals("Kedokteran") ? 1000000 : 250000);
            
            log.info("Student {} selected formula {}", email, formula);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error selecting formula: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Check submission status - apakah user sudah submit form atau belum
     */
    @GetMapping("/submission-status")
    public ResponseEntity<?> checkSubmissionStatus() {
        try {
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            // Find latest admission form
            java.util.List<AdmissionForm> forms = admissionFormRepository.findByStudent_Id(student.getId());
            
            Map<String, Object> response = new HashMap<>();
            
            if (forms == null || forms.isEmpty()) {
                // Belum ada submission
                response.put("hasSubmitted", false);
                response.put("status", "NOT_SUBMITTED");
                response.put("message", "Anda belum mengirim formulir pendaftaran");
                log.info("Student {} has not submitted any form", userEmail);
            } else {
                // Get the latest form
                AdmissionForm latestForm = forms.get(forms.size() - 1);
                
                response.put("hasSubmitted", true);
                response.put("status", latestForm.getStatus().toString());
                response.put("formId", latestForm.getId());
                response.put("submittedAt", latestForm.getSubmittedAt());
                response.put("message", "Formulir sudah dikirim");
                
                // Check if editable (H+1 rule - 24 hours after submission)
                if (latestForm.getSubmittedAt() != null) {
                    java.time.LocalDateTime submittedTime = latestForm.getSubmittedAt();
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.Duration timeDiff = java.time.Duration.between(submittedTime, now);
                    long hoursElapsed = timeDiff.toHours();
                    
                    boolean isEditable = hoursElapsed < 24;
                    response.put("isEditable", isEditable);
                    response.put("hoursElapsed", hoursElapsed);
                    response.put("hoursRemaining", Math.max(0, 24 - hoursElapsed));
                    
                    if (!isEditable) {
                        response.put("editMessage", "Formulir sudah tidak dapat diedit. Sudah melewati 24 jam sejak pengiriman. " +
                                "Jika perlu perubahan, hubungi: +62-123-456-7890");
                    } else {
                        response.put("editMessage", "Anda masih bisa mengedit formulir selama " + (24 - hoursElapsed) + " jam");
                    }
                    
                    log.info("Student {} form check - Status: {}, Submitted: {}, Editable: {}", 
                            userEmail, latestForm.getStatus(), submittedTime, isEditable);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking submission status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get saved admission form data (for viewing/editing)
     */
    @GetMapping("/admission-form")
    public ResponseEntity<?> getAdmissionFormData() {
        try {
            log.info("📋 Getting admission form data...");
            
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            log.info("  User email: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("❌ User not found: {}", userEmail);
                        return new RuntimeException("User tidak ditemukan");
                    });
            log.info("  User ID: {}", user.getId());
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> {
                        log.error("❌ Student profile not found for user ID: {}", user.getId());
                        return new RuntimeException("Profil siswa tidak ditemukan");
                    });
            log.info("  Student ID: {}", student.getId());
            
            // Find latest admission form
            java.util.List<AdmissionForm> forms = admissionFormRepository.findByStudent_Id(student.getId());
            log.info("  Found {} admission forms", forms != null ? forms.size() : 0);
            
            if (forms == null || forms.isEmpty()) {
                log.warn("⚠️ No admission forms found for student {}", student.getId());
                return ResponseEntity.ok()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Tidak ada formulir yang tersimpan");
                            put("data", null);
                        }});
            }
            
            // Return latest form with all fields
            AdmissionForm latestForm = forms.get(forms.size() - 1);
            log.info("✅ Returning admission form for student {}", student.getId());
            
            // Map to DTO to avoid circular references in serialization
            com.uhn.pmb.dto.AdmissionFormDTO formDTO = com.uhn.pmb.dto.AdmissionFormDTO.builder()
                    .id(latestForm.getId())
                    .studentId(student.getId())
                    
                    // Period info
                    .periodId(latestForm.getPeriod() != null ? latestForm.getPeriod().getId() : null)
                    .periodName(latestForm.getPeriod() != null ? latestForm.getPeriod().getName() : null)
                    .waveType(latestForm.getPeriod() != null && latestForm.getPeriod().getWaveType() != null 
                        ? latestForm.getPeriod().getWaveType().toString() 
                        : "REGULAR_TEST")  // Default: REGULAR_TEST
                    
                    // Selection Type info
                    .selectionTypeId(latestForm.getSelectionTypeId())
                    .selectionTypeName(latestForm.getJenisSeleksiId() != null ? latestForm.getJenisSeleksiId().toString() : null)
                    .jenisSeleksiId(latestForm.getJenisSeleksiId())
                    .formType(latestForm.getFormType() != null ? latestForm.getFormType().toString() : null)
                    
                    // ===== PILIHAN PROGRAM STUDI =====
                    .programStudi1(latestForm.getProgramStudi1())
                    .programStudi2(latestForm.getProgramStudi2())
                    .programStudi3(latestForm.getProgramStudi3())
                    .additionalInfo(latestForm.getAdditionalInfo())
                    
                    // ===== DATA PRIBADI =====
                    .fullName(latestForm.getFullName())
                    .nik(latestForm.getNik())
                    .addressMedan(latestForm.getAddressMedan())
                    .residenceInfo(latestForm.getResidenceInfo())
                    .subdistrict(latestForm.getSubdistrict())
                    .district(latestForm.getDistrict())
                    .city(latestForm.getCity())
                    .province(latestForm.getProvince())
                    .phoneNumber(latestForm.getPhoneNumber())
                    .email(latestForm.getEmail())
                    .birthPlace(latestForm.getBirthPlace())
                    .birthDate(latestForm.getBirthDate())
                    .gender(latestForm.getGender())
                    .religion(latestForm.getReligion())
                    .informationSource(latestForm.getInformationSource())
                    
                    // ===== DATA ORANG TUA - AYAH =====
                    .fatherNik(latestForm.getFatherNik())
                    .fatherName(latestForm.getFatherName())
                    .fatherBirthDate(latestForm.getFatherBirthDate())
                    .fatherEducation(latestForm.getFatherEducation())
                    .fatherOccupation(latestForm.getFatherOccupation())
                    .fatherIncome(latestForm.getFatherIncome())
                    .fatherPhone(latestForm.getFatherPhone())
                    .fatherStatus(latestForm.getFatherStatus())
                    
                    // ===== DATA ORANG TUA - IBU =====
                    .motherNik(latestForm.getMotherNik())
                    .motherName(latestForm.getMotherName())
                    .motherBirthDate(latestForm.getMotherBirthDate())
                    .motherEducation(latestForm.getMotherEducation())
                    .motherOccupation(latestForm.getMotherOccupation())
                    .motherIncome(latestForm.getMotherIncome())
                    .motherPhone(latestForm.getMotherPhone())
                    .motherStatus(latestForm.getMotherStatus())
                    
                    // ===== ALAMAT ORANG TUA =====
                    .parentSubdistrict(latestForm.getParentSubdistrict())
                    .parentCity(latestForm.getParentCity())
                    .parentProvince(latestForm.getParentProvince())
                    .parentPhone(latestForm.getParentPhone())
                    
                    // ===== DATA ASAL SEKOLAH =====
                    .schoolOrigin(latestForm.getSchoolOrigin())
                    .schoolMajor(latestForm.getSchoolMajor())
                    .schoolYear(latestForm.getSchoolYear())
                    .nisn(latestForm.getNisn())
                    .schoolCity(latestForm.getSchoolCity())
                    .schoolProvince(latestForm.getSchoolProvince())
                    
                    // ===== DOKUMEN PENDUKUNG =====
                    .photoIdPath(latestForm.getPhotoIdPath())
                    .certificatePath(latestForm.getCertificatePath())
                    .transcriptPath(latestForm.getTranscriptPath())
                    
                    // Status & Timestamps
                    .status(latestForm.getStatus() != null ? latestForm.getStatus().toString() : null)
                    .submittedAt(latestForm.getSubmittedAt())
                    .createdAt(latestForm.getCreatedAt())
                    .updatedAt(latestForm.getUpdatedAt())
                    .build();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", formDTO);
            response.put("message", "Data formulir berhasil diambil");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching admission form: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Error: " + e.getMessage());
                        put("error", e.getClass().getSimpleName());
                    }});
        }
    }

    /**
     * DEBUG ENDPOINT - Check authentication without requiring authentication
     * Helps diagnose 401 errors
     */
    @GetMapping("/debug-auth")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> debugAuth(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            Map<String, Object> debug = new HashMap<>();
            debug.put("auth_header_received", authHeader != null && !authHeader.isEmpty());
            debug.put("auth_header_value", authHeader != null ? (authHeader.length() > 10 ? authHeader.substring(0, 10) + "..." : authHeader) : "NONE");
            debug.put("security_context_principal", auth != null ? auth.getPrincipal() : "null");
            debug.put("security_context_authenticated", auth != null ? auth.isAuthenticated() : false);
            debug.put("security_context_authorities", auth != null ? auth.getAuthorities().toString() : "none");
            
            log.info("DEBUG AUTH CHECK:");
            log.info("  Auth Header: {}", debug.get("auth_header_received"));
            log.info("  Principal: {}", debug.get("security_context_principal"));
            log.info("  Authenticated: {}", debug.get("security_context_authenticated"));
            log.info("  Authorities: {}", debug.get("security_context_authorities"));
            
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            log.error("Error in debug endpoint: {}", e.getMessage());
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("error", e.getMessage());
                put("timestamp", java.time.LocalDateTime.now());
            }});
        }
    }

    /**
     * Get current authenticated student's admission form (minimal data for dashboard)
     * Used by dashboard to display selected gelombang and program studi
     * 
     * @return AdmissionForm with id, studentId, periodId, jenisSeleksiId, selectionTypeId
     */
    @GetMapping("/admission-forms/current")
    @PreAuthorize("hasRole('CAMABA')")
    public ResponseEntity<?> getCurrentAdmissionForm() {
        try {
            log.info("📋 [DASHBOARD] Getting current student admission form...");
            
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            log.info("  📧 User email: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("❌ User not found: {}", userEmail);
                        return new RuntimeException("User tidak ditemukan");
                    });
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> {
                        log.error("❌ Student profile not found for user ID: {}", user.getId());
                        return new RuntimeException("Profil siswa tidak ditemukan");
                    });
            
            log.info("  👤 Student ID: {}", student.getId());
            
            // Find latest admission form
            List<AdmissionForm> forms = admissionFormRepository.findByStudent_Id(student.getId());
            log.info("  📝 Found {} admission forms", forms != null ? forms.size() : 0);
            
            if (forms == null || forms.isEmpty()) {
                log.warn("⚠️ No admission forms found for student {}", student.getId());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Belum ada formulir yang tersimpan");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }
            
            // Get latest form
            AdmissionForm latestForm = forms.get(forms.size() - 1);
            log.info("✅ Found admission form ID: {}", latestForm.getId());
            
            // Build minimal response for dashboard
            Map<String, Object> formData = new HashMap<>();
            formData.put("id", latestForm.getId());
            formData.put("studentId", student.getId());
            
            // Period info for gelombang
            if (latestForm.getPeriod() != null) {
                formData.put("periodId", latestForm.getPeriod().getId());
                formData.put("periodName", latestForm.getPeriod().getName());
                // ✅ NEW: Add wave mode (EARLY_NO_TEST, RANKING_NO_TEST, REGULAR_TEST)
                String waveMode = latestForm.getPeriod().getWaveType() != null 
                    ? latestForm.getPeriod().getWaveType().toString() 
                    : "REGULAR_TEST";
                formData.put("waveMode", waveMode);
                log.info("  🎯 Period ID: {}, Name: {}, Wave Mode: {}", 
                    latestForm.getPeriod().getId(), 
                    latestForm.getPeriod().getName(),
                    waveMode);
            } else {
                formData.put("periodId", null);
                formData.put("periodName", null);
                formData.put("waveMode", null);
                log.warn("  ⚠️ Period info is null");
            }
            
            // JenisSeleksi info for program
            if (latestForm.getJenisSeleksiId() != null) {
                formData.put("jenisSeleksiId", latestForm.getJenisSeleksiId());
                
                // ✅ NEW: Look up jenis-seleksi name from database
                String jenisSeleksiName = null;
                try {
                    var jenisSeleksi = jenisSeleksiRepository.findById(latestForm.getJenisSeleksiId());
                    if (jenisSeleksi.isPresent()) {
                        jenisSeleksiName = jenisSeleksi.get().getNama();
                        log.info("  📚 JenisSeleksi ID: {}, Name: {}", latestForm.getJenisSeleksiId(), jenisSeleksiName);
                    } else {
                        log.warn("  ⚠️ JenisSeleksi ID {} not found in database", latestForm.getJenisSeleksiId());
                    }
                } catch (Exception e) {
                    log.warn("  ⚠️ Error looking up JenisSeleksi: {}", e.getMessage());
                }
                
                formData.put("jenisSeleksiName", jenisSeleksiName);
            } else {
                formData.put("jenisSeleksiId", null);
                formData.put("jenisSeleksiName", null);
                log.warn("  ⚠️ JenisSeleksi ID is null");
            }
            
            // Selection type
            formData.put("selectionTypeId", latestForm.getSelectionTypeId());
            
            // ✅ NEW: Add Program Studi choices (for cicilan payment page)
            formData.put("programStudi1", latestForm.getProgramStudi1());
            formData.put("programStudi2", latestForm.getProgramStudi2());
            formData.put("programStudi3", latestForm.getProgramStudi3());
            log.info("  🎓 Program Studi choices: {}, {}, {}", 
                latestForm.getProgramStudi1(), 
                latestForm.getProgramStudi2(), 
                latestForm.getProgramStudi3());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Data formulir berhasil diambil");
            response.put("data", formData);
            
            log.info("✅ [DASHBOARD] Successfully returning admission form for student {} with program studi choices", student.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [DASHBOARD] Error fetching current admission form: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Error: " + e.getMessage());
                        put("data", null);
                    }});
        }
    }

    /**
     * Alternative endpoint for getting current student's admission form (alias for /current)
     * Provides fallback option if /current is not available
     * 
     * @return Same response as /admission-forms/current
     */
    @GetMapping("/admission-forms/me")
    @PreAuthorize("hasRole('CAMABA')")
    public ResponseEntity<?> getMyAdmissionForm() {
        log.info("📋 [DASHBOARD] [FALLBACK] Redirecting /me to /current endpoint");
        return getCurrentAdmissionForm();
    }

    /**
     * Update admission form data (for editing)
     */
    @PutMapping(value = "/admission-form", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CAMABA')")
    public ResponseEntity<?> updateAdmissionFormData(
            HttpServletRequest request) {
        try {
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            // Find latest admission form
            java.util.List<AdmissionForm> forms = admissionFormRepository.findByStudent_Id(student.getId());
            
            if (forms == null || forms.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Tidak ada formulir yang tersimpan"));
            }
            
            // Update the latest form
            AdmissionForm latestForm = forms.get(forms.size() - 1);
            
            // Get multipart request parameters
            if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                org.springframework.web.multipart.MultipartHttpServletRequest multipart = 
                    (org.springframework.web.multipart.MultipartHttpServletRequest) request;
                
                // ===== PILIHAN PROGRAM STUDI =====
                String programStudi1 = multipart.getParameter("programStudi1");
                String programStudi2 = multipart.getParameter("programStudi2");
                String programStudi3 = multipart.getParameter("programStudi3");
                String additionalInfo = multipart.getParameter("additionalInfo");
                
                if (programStudi1 != null && !programStudi1.isEmpty()) {
                    latestForm.setProgramStudi1(programStudi1);
                }
                if (programStudi2 != null && !programStudi2.isEmpty()) {
                    latestForm.setProgramStudi2(programStudi2);
                }
                if (programStudi3 != null && !programStudi3.isEmpty()) {
                    latestForm.setProgramStudi3(programStudi3);
                }
                if (additionalInfo != null && !additionalInfo.isEmpty()) {
                    latestForm.setAdditionalInfo(additionalInfo);
                }
                
                // ===== DATA PRIBADI =====
                String fullName = multipart.getParameter("fullName");
                String nik = multipart.getParameter("nik");
                String addressMedan = multipart.getParameter("addressMedan");
                String residenceInfo = multipart.getParameter("residenceInfo");
                String subdistrict = multipart.getParameter("subdistrict");
                String district = multipart.getParameter("district");
                String city = multipart.getParameter("city");
                String province = multipart.getParameter("province");
                String phoneNumber = multipart.getParameter("phoneNumber");
                String email = multipart.getParameter("email");
                String birthPlace = multipart.getParameter("birthPlace");
                String birthDate = multipart.getParameter("birthDate");
                String gender = multipart.getParameter("gender");
                String religion = multipart.getParameter("religion");
                String informationSource = multipart.getParameter("informationSource");
                
                if (fullName != null && !fullName.isEmpty()) latestForm.setFullName(fullName);
                if (nik != null && !nik.isEmpty()) latestForm.setNik(nik);
                if (addressMedan != null && !addressMedan.isEmpty()) latestForm.setAddressMedan(addressMedan);
                if (residenceInfo != null && !residenceInfo.isEmpty()) latestForm.setResidenceInfo(residenceInfo);
                if (subdistrict != null && !subdistrict.isEmpty()) latestForm.setSubdistrict(subdistrict);
                if (district != null && !district.isEmpty()) latestForm.setDistrict(district);
                if (city != null && !city.isEmpty()) latestForm.setCity(city);
                if (province != null && !province.isEmpty()) latestForm.setProvince(province);
                if (phoneNumber != null && !phoneNumber.isEmpty()) latestForm.setPhoneNumber(phoneNumber);
                if (email != null && !email.isEmpty()) latestForm.setEmail(email);
                if (birthPlace != null && !birthPlace.isEmpty()) latestForm.setBirthPlace(birthPlace);
                if (birthDate != null && !birthDate.isEmpty()) latestForm.setBirthDate(birthDate);
                if (gender != null && !gender.isEmpty()) latestForm.setGender(gender);
                if (religion != null && !religion.isEmpty()) latestForm.setReligion(religion);
                if (informationSource != null && !informationSource.isEmpty()) latestForm.setInformationSource(informationSource);
                
                // ===== DATA ORANG TUA - AYAH =====
                String fatherNik = multipart.getParameter("fatherNik");
                String fatherName = multipart.getParameter("fatherName");
                String fatherBirthDate = multipart.getParameter("fatherBirthDate");
                String fatherEducation = multipart.getParameter("fatherEducation");
                String fatherOccupation = multipart.getParameter("fatherOccupation");
                String fatherIncome = multipart.getParameter("fatherIncome");
                String fatherPhone = multipart.getParameter("fatherPhone");
                String fatherStatus = multipart.getParameter("fatherStatus");
                
                if (fatherNik != null && !fatherNik.isEmpty()) latestForm.setFatherNik(fatherNik);
                if (fatherName != null && !fatherName.isEmpty()) latestForm.setFatherName(fatherName);
                if (fatherBirthDate != null && !fatherBirthDate.isEmpty()) latestForm.setFatherBirthDate(fatherBirthDate);
                if (fatherEducation != null && !fatherEducation.isEmpty()) latestForm.setFatherEducation(fatherEducation);
                if (fatherOccupation != null && !fatherOccupation.isEmpty()) latestForm.setFatherOccupation(fatherOccupation);
                if (fatherIncome != null && !fatherIncome.isEmpty()) latestForm.setFatherIncome(fatherIncome);
                if (fatherPhone != null && !fatherPhone.isEmpty()) latestForm.setFatherPhone(fatherPhone);
                if (fatherStatus != null && !fatherStatus.isEmpty()) latestForm.setFatherStatus(fatherStatus);
                
                // ===== DATA ORANG TUA - IBU =====
                String motherNik = multipart.getParameter("motherNik");
                String motherName = multipart.getParameter("motherName");
                String motherBirthDate = multipart.getParameter("motherBirthDate");
                String motherEducation = multipart.getParameter("motherEducation");
                String motherOccupation = multipart.getParameter("motherOccupation");
                String motherIncome = multipart.getParameter("motherIncome");
                String motherPhone = multipart.getParameter("motherPhone");
                String motherStatus = multipart.getParameter("motherStatus");
                
                if (motherNik != null && !motherNik.isEmpty()) latestForm.setMotherNik(motherNik);
                if (motherName != null && !motherName.isEmpty()) latestForm.setMotherName(motherName);
                if (motherBirthDate != null && !motherBirthDate.isEmpty()) latestForm.setMotherBirthDate(motherBirthDate);
                if (motherEducation != null && !motherEducation.isEmpty()) latestForm.setMotherEducation(motherEducation);
                if (motherOccupation != null && !motherOccupation.isEmpty()) latestForm.setMotherOccupation(motherOccupation);
                if (motherIncome != null && !motherIncome.isEmpty()) latestForm.setMotherIncome(motherIncome);
                if (motherPhone != null && !motherPhone.isEmpty()) latestForm.setMotherPhone(motherPhone);
                if (motherStatus != null && !motherStatus.isEmpty()) latestForm.setMotherStatus(motherStatus);
                
                // ===== ALAMAT ORANG TUA =====
                String parentSubdistrict = multipart.getParameter("parentSubdistrict");
                String parentCity = multipart.getParameter("parentCity");
                String parentProvince = multipart.getParameter("parentProvince");
                String parentPhone = multipart.getParameter("parentPhone");
                
                if (parentSubdistrict != null && !parentSubdistrict.isEmpty()) latestForm.setParentSubdistrict(parentSubdistrict);
                if (parentCity != null && !parentCity.isEmpty()) latestForm.setParentCity(parentCity);
                if (parentProvince != null && !parentProvince.isEmpty()) latestForm.setParentProvince(parentProvince);
                if (parentPhone != null && !parentPhone.isEmpty()) latestForm.setParentPhone(parentPhone);
                
                // ===== DATA ASAL SEKOLAH =====
                String schoolOrigin = multipart.getParameter("schoolOrigin");
                String schoolMajor = multipart.getParameter("schoolMajor");
                String schoolYearStr = multipart.getParameter("schoolYear");
                String nisn = multipart.getParameter("nisn");
                String schoolCity = multipart.getParameter("schoolCity");
                String schoolProvince = multipart.getParameter("schoolProvince");
                
                if (schoolOrigin != null && !schoolOrigin.isEmpty()) latestForm.setSchoolOrigin(schoolOrigin);
                if (schoolMajor != null && !schoolMajor.isEmpty()) latestForm.setSchoolMajor(schoolMajor);
                if (schoolYearStr != null && !schoolYearStr.isEmpty()) {
                    try {
                        latestForm.setSchoolYear(Integer.parseInt(schoolYearStr));
                    } catch (Exception e) {
                        log.warn("Error parsing schoolYear: {}", schoolYearStr);
                    }
                }
                if (nisn != null && !nisn.isEmpty()) latestForm.setNisn(nisn);
                if (schoolCity != null && !schoolCity.isEmpty()) latestForm.setSchoolCity(schoolCity);
                if (schoolProvince != null && !schoolProvince.isEmpty()) latestForm.setSchoolProvince(schoolProvince);
                
                // Create uploads directory if not exists
                String uploadsPath = "uploads/admission-forms/" + student.getId();
                Files.createDirectories(Paths.get(uploadsPath));
                
                // ===== HANDLE FILE UPLOADS =====
                MultipartFile photoId = multipart.getFile("photoId");
                if (photoId != null && !photoId.isEmpty()) {
                    String fileName = "photo_" + System.currentTimeMillis() + "_" + photoId.getOriginalFilename();
                    Path filePath = Paths.get(uploadsPath, fileName);
                    Files.write(filePath, photoId.getBytes());
                    latestForm.setPhotoIdPath("uploads/admission-forms/" + student.getId() + "/" + fileName);
                    log.info("Uploaded photo for student {}: {}", student.getId(), fileName);
                }
                
                MultipartFile certificate = multipart.getFile("certificate");
                if (certificate != null && !certificate.isEmpty()) {
                    String fileName = "certificate_" + System.currentTimeMillis() + "_" + certificate.getOriginalFilename();
                    Path filePath = Paths.get(uploadsPath, fileName);
                    Files.write(filePath, certificate.getBytes());
                    latestForm.setCertificatePath("uploads/admission-forms/" + student.getId() + "/" + fileName);
                    log.info("Uploaded certificate for student {}: {}", student.getId(), fileName);
                }
                
                MultipartFile transcript = multipart.getFile("transcript");
                if (transcript != null && !transcript.isEmpty()) {
                    String fileName = "transcript_" + System.currentTimeMillis() + "_" + transcript.getOriginalFilename();
                    Path filePath = Paths.get(uploadsPath, fileName);
                    Files.write(filePath, transcript.getBytes());
                    latestForm.setTranscriptPath("uploads/admission-forms/" + student.getId() + "/" + fileName);
                    log.info("Uploaded transcript for student {}: {}", student.getId(), fileName);
                }
            }
            
            latestForm.setUpdatedAt(LocalDateTime.now());
            admissionFormRepository.save(latestForm);
            
            // Convert Entity to DTO to avoid Hibernate lazy-loading serialization issues
            com.uhn.pmb.dto.AdmissionFormDTO formDTO = com.uhn.pmb.dto.AdmissionFormDTO.fromEntity(latestForm);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Formulir berhasil diperbarui dengan semua 48 field");
            response.put("data", formDTO);
            
            log.info("✅ Student {} updated admission form with COMPLETE data (48 editable fields)", userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error updating admission form data: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * TEST ENDPOINT - Debug selectionTypeId issue
     */
    @PostMapping("/test-selection-type")
    public ResponseEntity<?> testSelectionType(@RequestParam(required = false) String selectionTypeId,
                                               @RequestParam(required = false) String fullName,
                                               @RequestParam(required = false) Map<String, String> allParams) {
        try {
            log.info("===== TEST ENDPOINT CALLED =====");
            log.info("selectionTypeId param: '{}'", selectionTypeId);
            log.info("selectionTypeId is null? {}", selectionTypeId == null);
            log.info("selectionTypeId isEmpty? {}", selectionTypeId != null && selectionTypeId.isEmpty());
            log.info("selectionTypeId length: {}", selectionTypeId != null ? selectionTypeId.length() : "N/A");
            log.info("fullName: '{}'", fullName);
            
            if (allParams != null) {
                log.info("All params received: {}", allParams.keySet());
                if (allParams.containsKey("selectionTypeId")) {
                    log.info("  selectionTypeId from allParams: '{}'", allParams.get("selectionTypeId"));
                }
            }
            
            // Try to parse
            if (selectionTypeId != null && !selectionTypeId.isEmpty()) {
                try {
                    Long stId = Long.parseLong(selectionTypeId.trim());
                    log.info("✓ Parsed to Long: {}", stId);
                    
                    SelectionType st = selectionTypeRepository.findById(stId).orElse(null);
                    log.info("✓ Selection type found: {}", st != null ? st.getName() : "NOT FOUND");
                    
                    return ResponseEntity.ok(new HashMap<String, Object>() {{
                        put("success", true);
                        put("message", "SelectionTypeId received and parsed successfully");
                        put("received_selectionTypeId", selectionTypeId);
                        put("parsed_as_long", stId);
                        put("found_in_db", st != null);
                        put("selection_type_name", st != null ? st.getName() : null);
                    }});
                } catch (Exception e) {
                    log.error("Error parsing selectionTypeId: {}", e.getMessage(), e);
                    return ResponseEntity.badRequest()
                            .body(new HashMap<String, Object>() {{
                                put("error", "Failed to parse selectionTypeId");
                                put("message", e.getMessage());
                                put("received_value", selectionTypeId);
                            }});
                }
            } else {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("error", "selectionTypeId is null or empty");
                            put("received_value", selectionTypeId);
                        }});
            }
        } catch (Exception e) {
            log.error("Test endpoint error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Submit admission form with file uploads - ENHANCED WITH COMPLETE FIELD LOGGING
     * Maps ALL fields to their correct database columns (NEVER to additionalInfo JSON)
     */
    @PostMapping("/submit-admission-form")
    public ResponseEntity<?> submitAdmissionForm(@RequestHeader("Authorization") String token,
                                                 AdmissionFormSubmitRequest request) {
        try {
            log.info("╔═══════════════════════════════════════════════════════════════╗");
            log.info("║  SUBMIT ADMISSION FORM - ENHANCED FIELD MAPPING LOGGING       ║");
            log.info("╚═══════════════════════════════════════════════════════════════╝");
            
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // Create new admission form
            AdmissionForm form = new AdmissionForm();
            form.setStudent(student);
            form.setCreatedAt(LocalDateTime.now());
            
            // Track all fields being set
            StringBuilder fieldLog = new StringBuilder("\n📋 FIELD MAPPING LOG:\n");
            
            // ===== SET PERSONAL DATA - DIRECTLY TO DATABASE COLUMNS (NOT JSON) =====
            log.info("\n✅ SETTING PERSONAL DATA FIELDS:");
            
            form.setFullName(request.getFullName());
            fieldLog.append(String.format("  ✓ FULL_NAME: '%s'\n", request.getFullName()));
            
            form.setNik(request.getNik());
            fieldLog.append(String.format("  ✓ NIK: '%s'\n", request.getNik()));
            
            form.setAddressMedan(request.getAddressMedan());
            fieldLog.append(String.format("  ✓ ADDRESS_MEDAN: '%s'\n", request.getAddressMedan()));
            
            form.setResidenceInfo(request.getResidenceInfo());
            fieldLog.append(String.format("  ✓ RESIDENCE_INFO: '%s'\n", request.getResidenceInfo()));
            
            form.setSubdistrict(request.getSubdistrict());
            fieldLog.append(String.format("  ✓ SUBDISTRICT: '%s'\n", request.getSubdistrict()));
            
            form.setDistrict(request.getDistrict());
            fieldLog.append(String.format("  ✓ DISTRICT: '%s'\n", request.getDistrict()));
            
            form.setCity(request.getCity());
            fieldLog.append(String.format("  ✓ CITY: '%s'\n", request.getCity()));
            
            form.setProvince(request.getProvince());
            fieldLog.append(String.format("  ✓ PROVINCE: '%s'\n", request.getProvince()));
            
            form.setPhoneNumber(request.getPhoneNumber());
            fieldLog.append(String.format("  ✓ PHONE_NUMBER: '%s'\n", request.getPhoneNumber()));
            
            form.setEmail(request.getEmail());
            fieldLog.append(String.format("  ✓ EMAIL: '%s'\n", request.getEmail()));
            
            form.setBirthPlace(request.getBirthPlace());
            fieldLog.append(String.format("  ✓ BIRTH_PLACE: '%s'\n", request.getBirthPlace()));
            
            form.setBirthDate(request.getBirthDate());
            fieldLog.append(String.format("  ✓ BIRTH_DATE: '%s'\n", request.getBirthDate()));
            form.setGender(request.getGender());
            fieldLog.append(String.format("  ✓ GENDER: '%s'\n", request.getGender()));
            
            form.setReligion(request.getReligion());
            fieldLog.append(String.format("  ✓ RELIGION: '%s'\n", request.getReligion()));
            
            form.setInformationSource(request.getInformationSource());
            fieldLog.append(String.format("  ✓ INFORMATION_SOURCE: '%s'\n", request.getInformationSource()));
            
            // ===== SET FATHER DATA =====
            log.info("\n✅ SETTING FATHER DATA FIELDS:");
            form.setFatherNik(request.getFatherNik());
            fieldLog.append(String.format("  ✓ FATHER_NIK: '%s'\n", request.getFatherNik()));
            
            form.setFatherName(request.getFatherName());
            fieldLog.append(String.format("  ✓ FATHER_NAME: '%s'\n", request.getFatherName()));
            
            form.setFatherBirthDate(request.getFatherBirthDate());
            fieldLog.append(String.format("  ✓ FATHER_BIRTH_DATE: '%s'\n", request.getFatherBirthDate()));
            
            form.setFatherEducation(request.getFatherEducation());
            fieldLog.append(String.format("  ✓ FATHER_EDUCATION: '%s'\n", request.getFatherEducation()));
            
            form.setFatherOccupation(request.getFatherOccupation());
            fieldLog.append(String.format("  ✓ FATHER_OCCUPATION: '%s'\n", request.getFatherOccupation()));
            
            form.setFatherIncome(request.getFatherIncome());
            fieldLog.append(String.format("  ✓ FATHER_INCOME: '%s'\n", request.getFatherIncome()));
            
            form.setFatherPhone(request.getFatherPhone());
            fieldLog.append(String.format("  ✓ FATHER_PHONE: '%s'\n", request.getFatherPhone()));
            
            form.setFatherStatus(request.getFatherStatus());
            fieldLog.append(String.format("  ✓ FATHER_STATUS: '%s'\n", request.getFatherStatus()));
            
            // ===== SET MOTHER DATA =====
            log.info("\n✅ SETTING MOTHER DATA FIELDS:");
            form.setMotherNik(request.getMotherNik());
            fieldLog.append(String.format("  ✓ MOTHER_NIK: '%s'\n", request.getMotherNik()));
            
            form.setMotherName(request.getMotherName());
            fieldLog.append(String.format("  ✓ MOTHER_NAME: '%s'\n", request.getMotherName()));
            
            form.setMotherBirthDate(request.getMotherBirthDate());
            fieldLog.append(String.format("  ✓ MOTHER_BIRTH_DATE: '%s'\n", request.getMotherBirthDate()));
            
            form.setMotherEducation(request.getMotherEducation());
            fieldLog.append(String.format("  ✓ MOTHER_EDUCATION: '%s'\n", request.getMotherEducation()));
            
            form.setMotherOccupation(request.getMotherOccupation());
            fieldLog.append(String.format("  ✓ MOTHER_OCCUPATION: '%s'\n", request.getMotherOccupation()));
            
            form.setMotherIncome(request.getMotherIncome());
            fieldLog.append(String.format("  ✓ MOTHER_INCOME: '%s'\n", request.getMotherIncome()));
            
            form.setMotherPhone(request.getMotherPhone());
            fieldLog.append(String.format("  ✓ MOTHER_PHONE: '%s'\n", request.getMotherPhone()));
            
            form.setMotherStatus(request.getMotherStatus());
            fieldLog.append(String.format("  ✓ MOTHER_STATUS: '%s'\n", request.getMotherStatus()));
            
            // ===== SET PARENT ADDRESS =====
            log.info("\n✅ SETTING PARENT ADDRESS FIELDS:");
            form.setParentSubdistrict(request.getParentSubdistrict());
            fieldLog.append(String.format("  ✓ PARENT_SUBDISTRICT: '%s'\n", request.getParentSubdistrict()));
            
            form.setParentCity(request.getParentCity());
            fieldLog.append(String.format("  ✓ PARENT_CITY: '%s'\n", request.getParentCity()));
            
            form.setParentProvince(request.getParentProvince());
            fieldLog.append(String.format("  ✓ PARENT_PROVINCE: '%s'\n", request.getParentProvince()));
            
            form.setParentPhone(request.getParentPhone());
            fieldLog.append(String.format("  ✓ PARENT_PHONE: '%s'\n", request.getParentPhone()));
            
            // ===== SET SCHOOL DATA =====
            log.info("\n✅ SETTING SCHOOL DATA FIELDS:");
            form.setSchoolOrigin(request.getSchoolOrigin());
            fieldLog.append(String.format("  ✓ SCHOOL_ORIGIN: '%s'\n", request.getSchoolOrigin()));
            
            form.setSchoolMajor(request.getSchoolMajor());
            fieldLog.append(String.format("  ✓ SCHOOL_MAJOR: '%s'\n", request.getSchoolMajor()));
            
            form.setSchoolYear(request.getSchoolYear());
            fieldLog.append(String.format("  ✓ SCHOOL_YEAR: %d\n", request.getSchoolYear()));
            
            form.setNisn(request.getNisn());
            fieldLog.append(String.format("  ✓ NISN: '%s'\n", request.getNisn()));
            
            form.setSchoolCity(request.getSchoolCity());
            fieldLog.append(String.format("  ✓ SCHOOL_CITY: '%s'\n", request.getSchoolCity()));
            
            form.setSchoolProvince(request.getSchoolProvince());
            fieldLog.append(String.format("  ✓ SCHOOL_PROVINCE: '%s'\n", request.getSchoolProvince()));
            
            // ===== SET PROGRAM CHOICES =====
            log.info("\n✅ SETTING PROGRAM CHOICE FIELDS:");
            form.setProgramStudi1(request.getProgramChoice1());
            fieldLog.append(String.format("  ✓ PROGRAM_STUDI_1: '%s'\n", request.getProgramChoice1()));
            
            form.setProgramStudi2(request.getProgramChoice2());
            fieldLog.append(String.format("  ✓ PROGRAM_STUDI_2: '%s'\n", request.getProgramChoice2()));
            
            form.setProgramStudi3(request.getProgramChoice3());
            fieldLog.append(String.format("  ✓ PROGRAM_STUDI_3: '%s'\n", request.getProgramChoice3()));
            
            // ===== HANDLE FILE UPLOADS - DIRECT TO DATABASE COLUMNS =====
            log.info("\n✅ HANDLING FILE UPLOADS:");
            if (request.getPhotoId() != null && !request.getPhotoId().isEmpty()) {
                String photoPath = saveFile(request.getPhotoId(), "admission-forms", student.getId(), "photo");
                form.setPhotoIdPath(photoPath);
                fieldLog.append(String.format("  ✓ PHOTO_ID_PATH: '%s'\n", photoPath));
                log.info("✓ Photo saved: {}", photoPath);
            }
            
            if (request.getCertificate() != null && !request.getCertificate().isEmpty()) {
                String certPath = saveFile(request.getCertificate(), "admission-forms", student.getId(), "certificate");
                form.setCertificatePath(certPath);
                fieldLog.append(String.format("  ✓ CERTIFICATE_PATH: '%s'\n", certPath));
                log.info("✓ Certificate saved: {}", certPath);
            }
            
            if (request.getTranscript() != null && !request.getTranscript().isEmpty()) {
                String transcriptPath = saveFile(request.getTranscript(), "admission-forms", student.getId(), "transcript");
                form.setTranscriptPath(transcriptPath);
                fieldLog.append(String.format("  ✓ TRANSCRIPT_PATH: '%s'\n", transcriptPath));
                log.info("✓ Transcript saved: {}", transcriptPath);
            }
            
            // ===== SET JENIS SELEKSI (SELECTION TYPE) & PERIOD =====
            log.info("\n✅ SETTING JENIS SELEKSI (FORMULA/PROGRAM TYPE) & PERIOD:");
            
            // 🔥 FIX: Use jenisSeleksiId untuk lookup formula, selectionTypeId untuk lookup period
            Long periodId = request.getSelectionTypeId();
            Long jenisSeleksiIdFromRequest = request.getJenisSeleksiId();
            
            log.info("📍 Received from request:");
            log.info("   - selectionTypeId (= periodId): {}", periodId);
            log.info("   - jenisSeleksiId (= formula): {}", jenisSeleksiIdFromRequest);
            
            // ✅ NEW: Lookup JenisSeleksi using jenisSeleksiId (not selectionTypeId!)
            if (jenisSeleksiIdFromRequest == null || jenisSeleksiIdFromRequest <= 0) {
                log.error("❌ Jenis Seleksi (Formula) ID is required");
                throw new RuntimeException("Jenis Seleksi (Formula) ID adalah wajib!");
            }
            
            JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiIdFromRequest)
                    .orElseThrow(() -> new RuntimeException(
                            "Jenis Seleksi (Formula) dengan ID " + jenisSeleksiIdFromRequest + " tidak ditemukan"));
            
            // ✅ Store jenisSeleksiId directly in form
            form.setJenisSeleksiId(jenisSeleksi.getId());
            
            // ✅ Determine form type based on jenis seleksi code/name
            // If code is "MEDICAL" or name contains "Kedokteran", use MEDICAL form type
            boolean isMedicalFormula = jenisSeleksi.getCode() != null && jenisSeleksi.getCode().equalsIgnoreCase("MEDICAL") ||
                                      jenisSeleksi.getNama() != null && jenisSeleksi.getNama().toLowerCase().contains("kedokteran");
            SelectionType.FormType formType = isMedicalFormula ? SelectionType.FormType.MEDICAL : SelectionType.FormType.NON_MEDICAL;
            form.setFormType(formType);
            fieldLog.append(String.format("  ✓ JENIS_SELEKSI_ID: %d (%s)\n", jenisSeleksi.getId(), jenisSeleksi.getNama()));
            fieldLog.append(String.format("  ✓ FORM_TYPE: %s (Medical: %s)\n", formType, isMedicalFormula));
            log.info("✓ Jenis Seleksi SET: {} (Medical: {})", jenisSeleksi.getId(), isMedicalFormula);
            
            // 🔥 SET PERIOD_ID BASED ON selectionTypeId (which is periodId from gelombang)
            log.info("\n🔥 [CRITICAL-STRICT] Looking up PERIOD (Registration Period) with ID: {}", periodId);
            
            // ✅ STRICT: periodId MUST be valid and > 0
            if (periodId == null || periodId <= 0) {
                log.error("❌ FATAL: periodId is null or <= 0!");
                throw new RuntimeException("Period/Gelombang ID harus valid. Silakan mulai dari gelombang-selection.html");
            }
            
            // ✅ STRICT: Period MUST exist in database
            RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                    .orElseThrow(() -> {
                        log.error("❌ FATAL: Period dengan ID {} tidak ditemukan di database", periodId);
                        return new RuntimeException("Period/Gelombang dengan ID " + periodId + " tidak ditemukan");
                    });
            
            form.setPeriod(period);
            form.setJenisSeleksiId(jenisSeleksiIdFromRequest);
            form.setSelectionTypeId(jenisSeleksiIdFromRequest);  // ✅ SELECTION_TYPE_ID = jenisSeleksiId
            fieldLog.append(String.format("  ✓ PERIOD_ID (Gelombang): %d (%s)\n", period.getId(), period.getName()));
            fieldLog.append(String.format("  ✓ JENIS_SELEKSI_ID (Jenis Seleksi): %d\n", jenisSeleksiIdFromRequest));
            fieldLog.append(String.format("  ✓ SELECTION_TYPE_ID: %d\n", jenisSeleksiIdFromRequest));
            log.info("✓ PERIOD (Gelombang) SET: {} ({})", period.getId(), period.getName());
            log.info("✓ JENIS_SELEKSI_ID SET: {}", jenisSeleksiIdFromRequest);
            log.info("✓ SELECTION_TYPE_ID SET: {}", jenisSeleksiIdFromRequest);
            
            // ===== SET STATUS & TIMESTAMPS =====
            log.info("\n✅ SETTING STATUS & TIMESTAMPS:");
            form.setStatus(AdmissionForm.FormStatus.VERIFIED);
            form.setSubmittedAt(LocalDateTime.now());
            form.setUpdatedAt(LocalDateTime.now());
            fieldLog.append(String.format("  ✓ STATUS: %s\n", AdmissionForm.FormStatus.VERIFIED));
            fieldLog.append(String.format("  ✓ SUBMITTED_AT: %s\n", form.getSubmittedAt()));
            fieldLog.append(String.format("  ✓ UPDATED_AT: %s\n", form.getUpdatedAt()));
            
            // ===== CRITICAL: DO NOT USE additionalInfo FOR FORM DATA! =====
            // Ensure additionalInfo stays null or empty - NEVER serialize JSON data here
            form.setAdditionalInfo(null);  // Explicitly prevent JSON abuse
            log.info("\n⚠️  VERIFICATION: additionalInfo set to NULL (no JSON data stored)\n");
            
            // ✅ NEW: HANDLE FILE UPLOADS
            log.info("\n🔄 PROCESSING FILE UPLOADS...");
            try {
                // Save required files
                if (request.getPhotoId() != null) {
                    String photoPath = saveFile(request.getPhotoId(), "admission-forms", student.getId(), "photoId");
                    form.setPhotoIdPath(photoPath);
                    log.info("  ✓ PhotoId uploaded: {}", photoPath);
                }
                
                if (request.getCertificate() != null) {
                    String certPath = saveFile(request.getCertificate(), "admission-forms", student.getId(), "certificate");
                    form.setCertificatePath(certPath);
                    log.info("  ✓ Certificate uploaded: {}", certPath);
                }
                
                if (request.getTranscript() != null) {
                    String transcriptPath = saveFile(request.getTranscript(), "admission-forms", student.getId(), "transcript");
                    form.setTranscriptPath(transcriptPath);
                    log.info("  ✓ Transcript uploaded: {}", transcriptPath);
                }
                
                // ✅ NEW: Save optional files for RANKING_NO_TEST wave
                if (request.getNilaiFile() != null) {
                    String nilaiPath = saveFile(request.getNilaiFile(), "admission-forms", student.getId(), "nilaiFile");
                    form.setNilaiFilePath(nilaiPath);
                    log.info("  ✓ Nilai file uploaded: {}", nilaiPath);
                }
                
                if (request.getRankingFile() != null) {
                    String rankingPath = saveFile(request.getRankingFile(), "admission-forms", student.getId(), "rankingFile");
                    form.setRankingFilePath(rankingPath);
                    log.info("  ✓ Ranking file uploaded: {}", rankingPath);
                }
                
                log.info("✅ ALL FILE UPLOADS COMPLETED\n");
            } catch (Exception e) {
                log.warn("Error uploading files: {}", e.getMessage());
                // Don't fail submission if file upload fails - continue with form save
            }
            
            // ===== Save form to database =====
            log.info("\n🔄 SAVING FORM TO DATABASE...");
            AdmissionForm savedForm = admissionFormRepository.save(form);
            log.info("✅ FORM SAVED SUCCESSFULLY - ID: {}", savedForm.getId());
            log.info(fieldLog.toString());
            log.info("\n✅ TOTAL FIELDS MAPPED & SAVED: 48+\n");

            // 🔴 CREATE FORMVALIDATION RECORD (so admin can see it in dashboard)
            FormValidation formValidation = FormValidation.builder()
                    .admissionForm(form)
                    .student(student)
                    .validationStatus(FormValidation.ValidationStatus.PENDING)
                    .paymentStatus(FormValidation.PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            formValidation = formValidationRepository.save(formValidation);
            log.info("✅ FormValidation record created for form: {} with status PENDING", form.getId());

            // ✅ NEW: CREATE FORM REPAIR STATUS RECORD (separate table for repair tracking)
            FormRepairStatus repairStatus = FormRepairStatus.builder()
                    .formValidation(formValidation)
                    .status(FormRepairStatus.RepairStatus.BELUM_PERBAIKAN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            formRepairStatusRepository.save(repairStatus);
            log.info("✅ FormRepairStatus record created with status BELUM_PERBAIKAN for form validation: {}", formValidation.getId());

            // 🟢 CREATE REGISTRATION STATUS FOR FORM_SUBMISSION STAGE (FIX: So dashboard can track status)
            RegistrationStatus formStatus = registrationStatusService.markAsCompleted(
                    user, 
                    RegistrationStatus.RegistrationStage.FORM_SUBMISSION,
                    "Form submitted at " + LocalDateTime.now()
            );
            log.info("✅ RegistrationStatus created for stage FORM_SUBMISSION with status SELESAI for user: {}", user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Formulir pendaftaran berhasil disubmit. Data Anda telah terverifikasi!");
            response.put("formId", form.getId());
            response.put("status", "VERIFIED");
            response.put("estimatedDays", "3-5 hari kerja");
            
            log.info("Student {} submitted admission form: {} with status SUBMITTED and files uploaded", userEmail, form.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error submitting admission form: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * 🔥 NEW ENDPOINT: Update admission form with selected period & formula
     * Called after user selects gelombang (periodId) and formula (jenisSeleksiId)
     * This ensures PERIOD_ID and JENIS_SELEKSI_ID are saved BEFORE form submission
     */
    @PostMapping("/admission-forms/update-selection")
    public ResponseEntity<?> updateAdmissionFormSelection(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("╔═══════════════════════════════════════════════════════════════╗");
            log.info("║  UPDATE ADMISSION FORM - SET PERIOD & FORMULA (JENIS_SELEKSI)  ║");
            log.info("╚═══════════════════════════════════════════════════════════════╝");
            
            // Extract periodId (from gelombang) and jenisSeleksiId (from formula)
            Long periodId = null;
            Long jenisSeleksiId = null;
            Long gelombangId = null;
            Long selectionTypeId = null;
            
            if (request.containsKey("periodId")) {
                Object p = request.get("periodId");
                periodId = p instanceof Number ? ((Number) p).longValue() : Long.parseLong(String.valueOf(p));
            }
            if (request.containsKey("gelombangId")) {
                Object g = request.get("gelombangId");
                gelombangId = g instanceof Number ? ((Number) g).longValue() : Long.parseLong(String.valueOf(g));
            }
            if (request.containsKey("jenisSeleksiId")) {
                Object j = request.get("jenisSeleksiId");
                jenisSeleksiId = j instanceof Number ? ((Number) j).longValue() : Long.parseLong(String.valueOf(j));
            }
            if (request.containsKey("selectionTypeId")) {
                Object s = request.get("selectionTypeId");
                selectionTypeId = s instanceof Number ? ((Number) s).longValue() : Long.parseLong(String.valueOf(s));
            }
            
            log.info("📊 [INPUT] periodId: {}, gelombangId: {}, jenisSeleksiId: {}, selectionTypeId: {}", 
                    periodId, gelombangId, jenisSeleksiId, selectionTypeId);
            
            // Normalize: if periodId is null, use gelombangId (they should be same)
            if (periodId == null && gelombangId != null) {
                periodId = gelombangId;
                log.info("📝 [NORMALIZE] Using gelombangId as periodId: {}", periodId);
            }
            
            // Normalize: if jenisSeleksiId is null, use selectionTypeId
            if (jenisSeleksiId == null && selectionTypeId != null) {
                jenisSeleksiId = selectionTypeId;
                log.info("📝 [NORMALIZE] Using selectionTypeId as jenisSeleksiId: {}", jenisSeleksiId);
            }
            
            // Get current user
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            log.info("👤 [USER] Email: {}, Student ID: {}", userEmail, student.getId());
            
            // Find latest admission form for this student
            List<AdmissionForm> forms = admissionFormRepository.findByStudent_Id(student.getId());
            if (forms.isEmpty()) {
                log.warn("⚠️  No admission form found for student {}", student.getId());
                return ResponseEntity.ok(new ApiResponse(true, "No admission form to update (not a critical error)"));
            }
            
            // Get the latest form (last created)
            AdmissionForm form = forms.get(forms.size() - 1);
            log.info("📋 [FORM] Found form ID: {} (created at: {})", form.getId(), form.getCreatedAt());
            
            // Update PERIOD_ID if periodId provided
            if (periodId != null && periodId > 0) {
                RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                        .orElse(null);
                
                if (period != null) {
                    form.setPeriod(period);
                    log.info("✅ [UPDATE] PERIOD_ID set to: {} ({})", periodId, period.getName());
                } else {
                    log.warn("⚠️  RegistrationPeriod with ID {} not found, skipping period update", periodId);
                }
            } else {
                log.warn("⚠️  No periodId provided, skipping period update");
            }
            
            // Update JENIS_SELEKSI_ID if jenisSeleksiId provided
            if (jenisSeleksiId != null && jenisSeleksiId > 0) {
                JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiId)
                        .orElse(null);
                
                if (jenisSeleksi != null) {
                    form.setJenisSeleksiId(jenisSeleksi.getId());
                    log.info("✅ [UPDATE] JENIS_SELEKSI_ID set to: {} ({})", jenisSeleksiId, jenisSeleksi.getNama());
                    
                    // Also determine form type based on jenis seleksi
                    boolean isMedicalFormula = jenisSeleksi.getCode() != null && jenisSeleksi.getCode().equalsIgnoreCase("MEDICAL") ||
                                              jenisSeleksi.getNama() != null && jenisSeleksi.getNama().toLowerCase().contains("kedokteran");
                    SelectionType.FormType formType = isMedicalFormula ? SelectionType.FormType.MEDICAL : SelectionType.FormType.NON_MEDICAL;
                    form.setFormType(formType);
                    log.info("✅ [UPDATE] FORM_TYPE set to: {} (Medical: {})", formType, isMedicalFormula);
                } else {
                    log.warn("⚠️  JenisSeleksi with ID {} not found, skipping jenis seleksi update", jenisSeleksiId);
                }
            } else {
                log.warn("⚠️  No jenisSeleksiId provided, skipping jenis seleksi update");
            }
            
            // Update timestamps
            form.setUpdatedAt(LocalDateTime.now());
            
            // Save updated form
            AdmissionForm updatedForm = admissionFormRepository.save(form);
            log.info("💾 [SAVED] Admission form updated and saved, ID: {}", updatedForm.getId());
            
            // Build response with details
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Admission form updated successfully");
            response.put("formId", updatedForm.getId());
            response.put("periodId", periodId);
            response.put("jenisSeleksiId", jenisSeleksiId);
            response.put("updatedAt", updatedForm.getUpdatedAt());
            
            log.info("✅ [RESPONSE] Update successful: {}", response);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [ERROR] Error updating admission form selection: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating form: " + e.getMessage()));
        }
    }

    /**
     * Helper method to save uploaded files
     */
    private String saveFile(MultipartFile file, String directory, Long studentId, String fileType) throws Exception {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Create directory structure: uploads/admission-forms/{studentId}/
        String baseDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + directory + 
                       File.separator + studentId;
        Path uploadPath = Paths.get(baseDir);
        
        // Create directories if they don't exist
        Files.createDirectories(uploadPath);

        // Generate filename with timestamp to avoid duplicates
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bin";
        String filename = fileType + "_" + System.currentTimeMillis() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Return relative path for storage in database
        return "uploads/" + directory + "/" + studentId + "/" + filename;
    }

    /**
     * Submit exam answers
     */
    @PostMapping("/submit-exam")
    public ResponseEntity<?> submitExam(@RequestBody Map<String, Object> request) {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // Create exam record
            Exam exam = new Exam();
            exam.setStudent(student);
            exam.setStartedAt(java.time.LocalDateTime.now());
            exam.setStatus(Exam.ExamStatus.COMPLETED);
            exam.setCreatedAt(java.time.LocalDateTime.now());
            
            examRepository.save(exam);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ujian berhasil disubmit");
            response.put("examId", exam.getId());
            
            log.info("Student {} submitted exam", email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error submitting exam: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Verify payment - Mark payment as completed in backend
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        try {
            String vaNumber = request.get("vaNumber");
            
            if (vaNumber == null || vaNumber.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "VA Number is required"));
            }

            // Get current user
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Find VirtualAccount by VA number
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("Virtual Account not found"));
            
            // Mark VirtualAccount as paid
            va.setPaidAt(LocalDateTime.now());
            va.setStatus(VirtualAccount.VAStatus.PAID);
            va.setUpdatedAt(LocalDateTime.now());
            virtualAccountRepository.save(va);
            
            log.info("✅ Virtual Account {} marked as PAID", vaNumber);
            
            // Update RegistrationStatus for PAYMENT_BRIVA
            Optional<RegistrationStatus> existingStatus = registrationStatusRepository
                    .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_BRIVA);
            
            RegistrationStatus paymentStatus;
            if (existingStatus.isPresent()) {
                paymentStatus = existingStatus.get();
                paymentStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                paymentStatus.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new status if doesn't exist
                paymentStatus = new RegistrationStatus();
                paymentStatus.setUser(user);
                paymentStatus.setStage(RegistrationStatus.RegistrationStage.PAYMENT_BRIVA);
                paymentStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                paymentStatus.setCreatedAt(LocalDateTime.now());
                paymentStatus.setUpdatedAt(LocalDateTime.now());
            }
            
            registrationStatusRepository.save(paymentStatus);
            
            log.info("✅ Payment status updated to SELESAI for user: {}", email);
            
            // ✅ AUTO-GENERATE EXAM TOKEN AFTER PAYMENT VERIFIED
            log.info("🎫 [VERIFY-PAYMENT] Attempting to generate exam token...");
            try {
                // Call the helper method to update FormValidation and generate token
                updateFormValidationPaymentStatusFromSimulate(va);
                log.info("✅ [VERIFY-PAYMENT] Token generation completed");
            } catch (Exception tokenError) {
                log.error("❌ [VERIFY-PAYMENT] Error generating token: {}", tokenError.getMessage(), tokenError);
                // Don't fail payment verification if token generation fails
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pembayaran berhasil diverifikasi dan disimpan di database");
            response.put("vaNumber", vaNumber);
            response.put("status", "SELESAI");
            response.put("paidAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error verifying payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Register for admission
     */
    @PostMapping("/register-admission")
    public ResponseEntity<?> registerForAdmission(@RequestBody RegistrationRequest request) {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            RegistrationPeriod period = registrationPeriodRepository.findById(request.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period not found"));
            
            SelectionType selectionType = selectionTypeRepository.findById(request.getSelectionTypeId())
                    .orElseThrow(() -> new RuntimeException("Selection type not found"));
            
            AdmissionForm form = registrationService.registerForAdmission(
                    student, period, selectionType, request.getProgramStudi());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(form);
        } catch (Exception e) {
            log.error("Error registering for admission: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Buy form and create virtual account
     */
    @PostMapping("/buy-form/{formId}")
    public ResponseEntity<?> buyForm(@PathVariable Long formId) {
        try {
            AdmissionForm form = admissionFormRepository.findById(formId)
                    .orElseThrow(() -> new RuntimeException("Form not found"));
            
            // Check if form is already paid
            if (form.getStatus() == AdmissionForm.FormStatus.WAITING_PAYMENT) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Form ini sudah dalam tahap pembayaran"));
            }
            
            // Check if form is VERIFIED before allowing payment
            if (form.getStatus() != AdmissionForm.FormStatus.VERIFIED) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Formulir belum diverifikasi. Status saat ini: " + form.getStatus() + 
                                ". Silahkan menunggu hingga data Anda diverifikasi (3-5 hari kerja)."));
            }
            
            form.setStatus(AdmissionForm.FormStatus.WAITING_PAYMENT);
            admissionFormRepository.save(form);
            
            VirtualAccount va = registrationService.buyFormAndCreateVA(form);
            
            // Send email notification
            emailService.sendVirtualAccountInfo(
                    form.getStudent().getUser().getEmail(),
                    va.getVaNumber(),
                    va.getAmount().toString(),
                    va.getExpiredAt().toString()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Form siap untuk pembayaran");
            response.put("vaNumber", va.getVaNumber());
            response.put("amount", va.getAmount());
            response.put("expiredAt", va.getExpiredAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error buying form: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get admission status - Returns array of forms for current user (ONLY their own data)
     */
    @GetMapping("/admission-status")
    public ResponseEntity<?> getAdmissionStatus() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            // Get forms - sorted by most recent first (for dashboard to use latest)
            List<AdmissionForm> forms = admissionFormRepository.findByStudent_Id(student.getId());
            forms.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())); // Sort descending
            
            log.info("✓ Fetched {} admission forms for user: {}", forms.size(), email);
            
            // Return as array directly (NOT wrapped in object)
            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            log.error("Error fetching admission status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Password baru tidak cocok"));
            }

            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify old password
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            if (!encoder.matches(request.getOldPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Password lama tidak sesuai"));
            }

            // Update password
            user.setPassword(encoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponse(true, "Password berhasil diubah"));
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get exam details
     */
    @GetMapping("/exam")
    public ResponseEntity<?> getExamDetails() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            Exam exam = examRepository.findByStudent_Id(student.getId()).orElse(null);
            
            if (exam == null) {
                return ResponseEntity.ok(new ApiResponse(false, "No exam found"));
            }
            
            return ResponseEntity.ok(exam);
        } catch (Exception e) {
            log.error("Error fetching exam: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get exam validation status for current camaba
     * GET /api/camaba/exam-validation-status
     */
    @GetMapping("/exam-validation-status")
    public ResponseEntity<?> getExamValidationStatus() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            Optional<ExamResult> resultOpt = examResultRepository.findByStudent_Id(student.getId());
            
            if (!resultOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("hasExamResult", false);
                response.put("validationStatus", null);
                return ResponseEntity.ok(response);
            }
            
            ExamResult result = resultOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hasExamResult", true);
            response.put("validationStatus", result.getExamValidationStatus().toString());
            response.put("adminNotes", result.getAdminNotes());
            response.put("examValidatedAt", result.getExamValidatedAt());
            response.put("tokenValidated", result.getTokenValidated());
            response.put("gformScore", result.getGformScore());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting exam validation status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // Inner class for requests
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RegistrationRequest {
        private Long periodId;
        private Long selectionTypeId;
        private String programStudi;
    }

    /**
     * Create Virtual Account for form payment
     * POST /api/camaba/create-virtual-account
     * Body: { selectionTypeId (actually jenisSeleksiId), periodId, amount }
     */
    @PostMapping("/create-virtual-account")
    public ResponseEntity<?> createVirtualAccount(@RequestBody Map<String, Object> request) {
        try {
            Long selectionTypeId = ((Number) request.get("selectionTypeId")).longValue();
            Long periodId = ((Number) request.get("periodId")).longValue();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // ✅ FIXED: Query from JenisSeleksi (table jenis_seleksi) not SelectionType (table selection_types which is empty)
            JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(selectionTypeId)
                    .orElseThrow(() -> new RuntimeException("Jenis seleksi (Selection type) not found with ID: " + selectionTypeId));

            RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new RuntimeException("Registration period not found"));

            // Create Virtual Account
            VirtualAccount va = VirtualAccount.builder()
                    .student(student)
                    .amount(amount)
                    .paymentType(VirtualAccount.PaymentType.REGISTRATION_FORM)
                    .status(VirtualAccount.VAStatus.ACTIVE)
                    .build();

            // Generate VA number via BRIVA service
            String vaNumber = brivaService.generateVirtualAccount(va);
            va.setVaNumber(vaNumber);

            VirtualAccount savedVA = virtualAccountRepository.save(va);

            log.info("✅ Virtual Account created for student {}: {} (Rp {})", 
                    student.getId(), vaNumber, amount);

            // ✅ UPDATE FormValidation with VA info
            try {
                List<FormValidation> validations = formValidationRepository.findAll().stream()
                        .filter(fv -> fv.getStudent().getId().equals(student.getId()) 
                                && fv.getValidationStatus() == FormValidation.ValidationStatus.PENDING)
                        .limit(1)
                        .toList();
                
                if (!validations.isEmpty()) {
                    FormValidation fv = validations.get(0);
                    fv.setVirtualAccountNumber(savedVA.getVaNumber());
                    fv.setPaymentAmount(amount.longValue());
                    fv.setUpdatedAt(LocalDateTime.now());
                    formValidationRepository.save(fv);
                    log.info("✅ FormValidation updated with VA: {}, Amount: Rp {}", vaNumber, amount);
                }
            } catch (Exception e) {
                log.warn("⚠️ Warning updating FormValidation with VA info: {}", e.getMessage());
                // Don't fail VA creation if FV update fails
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Virtual Account berhasil dibuat");
            response.put("data", new HashMap<String, Object>() {{
                put("id", savedVA.getId());
                put("vaNumber", savedVA.getVaNumber());
                put("amount", savedVA.getAmount());
                put("status", savedVA.getStatus());
                put("createdAt", savedVA.getCreatedAt());
                put("expiredAt", savedVA.getExpiredAt());
                put("paymentType", savedVA.getPaymentType());
            }});

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error creating virtual account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", e.getMessage());
                    }});
        }
    }

    /**
     * Check payment status realtime
     * GET /api/camaba/check-payment-status?vaNumber=XXX
     * Returns payment status: WAITING, PAID, LUNAS, EXPIRED
     */
    @GetMapping("/check-payment-status")
    public ResponseEntity<?> checkPaymentStatus(@RequestParam String vaNumber) {
        try {
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("Virtual Account not found"));

            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            // Verify this VA belongs to the current user
            if (!va.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.status(403)
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Access denied");
                        }});
            }

            // Check if expired
            if (LocalDateTime.now().isAfter(va.getExpiredAt()) && 
                va.getStatus() != VirtualAccount.VAStatus.PAID) {
                va.setStatus(VirtualAccount.VAStatus.EXPIRED);
                virtualAccountRepository.save(va);
            }

            String status = va.getStatus().toString();
            
            // For PAID status, update related form
            if (va.getStatus() == VirtualAccount.VAStatus.PAID) {
                // Find and update associated admission form
                if (va.getAdmissionForm() != null) {
                    AdmissionForm form = va.getAdmissionForm();
                    form.setStatus(AdmissionForm.FormStatus.VERIFIED);
                    admissionFormRepository.save(form);
                    log.info("✅ Form status updated to VERIFIED for payment: {}", vaNumber);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", status);
            response.put("vaNumber", vaNumber);
            response.put("amount", va.getAmount());
            response.put("paidAt", va.getPaidAt());
            response.put("expiredAt", va.getExpiredAt());
            response.put("createdAt", va.getCreatedAt());

            log.info("✓ Payment status check for {}: {}", vaNumber, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error checking payment status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", e.getMessage());
                    }});
        }
    }

    /**
     * ✅ NEW: GET student's current exam token
     * GET /api/camaba/exam-token
     * 
     * Returns the student's active exam token if available
     */
    @GetMapping("/exam-token")
    public ResponseEntity<?> getExamToken() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            
            // Get active exam token
            // Use findAllByStudentId to handle cases where multiple tokens exist for same student
            List<ExamToken> tokens = tokenRepository.findAllByStudentId(student.getId());
            
            // Filter for active tokens
            ExamToken activeToken = tokens.stream()
                    .filter(ExamToken::isActive)
                    .findFirst()
                    .orElse(null);
            
            if (activeToken != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("tokenValue", activeToken.getTokenValue());
                response.put("expiresAt", activeToken.getExpiresAt());
                response.put("studentId", student.getId());
                response.put("message", "Token berhasil diambil");
                
                log.info("✅ Exam token retrieved for student: {} (ID: {})", student.getFullName(), student.getId());
                return ResponseEntity.ok(response);
            } else {
                // Check if there are any expired tokens for this student
                if (!tokens.isEmpty()) {
                    return ResponseEntity.status(404)
                            .body(new HashMap<String, Object>() {{
                                put("success", false);
                                put("message", "Token sudah expired atau tidak aktif");
                            }});
                } else {
                    return ResponseEntity.status(404)
                            .body(new HashMap<String, Object>() {{
                                put("success", false);
                                put("message", "Token ujian belum tersedia. Tunggu pembayaran Anda diproses.");
                            }});
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error getting exam token: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", e.getMessage());
                    }});
        }
    }

    /**
     * Simulate payment for testing (BRIVA callback simulation)
     * POST /api/camaba/simulate-payment?vaNumber=XXX
     */
    @PostMapping("/simulate-payment")
    public ResponseEntity<?> simulatePayment(@RequestParam String vaNumber) {
        try {
            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("Virtual Account not found"));

            // Update payment status
            brivaService.updatePaymentStatus(vaNumber, UUID.randomUUID().toString(), va.getAmount());

            // Verify and reload
            VirtualAccount updated = virtualAccountRepository.findByVaNumber(vaNumber)
                    .orElseThrow(() -> new RuntimeException("VA not found after update"));

            log.info("✅ Payment simulated for VA: {} - Status: {}", vaNumber, updated.getStatus());

            // ✅ NEW: Also auto-update FormValidation.paymentStatus to PAID (instant feedback for testing)
            updateFormValidationPaymentStatusFromSimulate(updated);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment simulated successfully");
            response.put("status", updated.getStatus());
            response.put("paidAt", updated.getPaidAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error simulating payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", e.getMessage());
                    }});
        }
    }

    /**
     * ✅ Helper: Auto-update FormValidation.paymentStatus when payment simulated
     * ✅ NEW: Also auto-generate exam token instantly (for testing)
     * Ensures instant feedback for testing (same as scheduler but immediate)
     */
    private void updateFormValidationPaymentStatusFromSimulate(VirtualAccount va) {
        try {
            if (va.getAdmissionForm() == null) {
                log.warn("⚠️ [SIMULATE] Cannot update FormValidation: AdmissionForm is null");
                return;
            }

            if (va.getStudent() == null) {
                log.warn("⚠️ [SIMULATE] Cannot update FormValidation: Student is null");
                return;
            }

            // Find FormValidation by admission form ID
            Optional<FormValidation> validationOpt = formValidationRepository
                    .findByAdmissionFormId(va.getAdmissionForm().getId());

            if (validationOpt.isPresent()) {
                FormValidation validation = validationOpt.get();
                
                // Update payment status to PAID
                validation.setPaymentStatus(FormValidation.PaymentStatus.PAID);
                validation.setPaymentDate(LocalDateTime.now());
                validation.setUpdatedAt(LocalDateTime.now());
                formValidationRepository.save(validation);

                log.info("✅ [SIMULATE] FormValidation #{} payment status updated to PAID for VA: {}", 
                        validation.getId(), va.getVaNumber());

                // ✅ IMMEDIATE: AUTO-GENERATE Exam Token instantly (don't wait for scheduler)
                // ✅ FIX: Check if token already exists to prevent duplicates
                try {
                    log.info("🎫 [SIMULATE-TOKEN-GEN] Checking for existing token for student {} (ID: {})...", 
                            va.getStudent().getFullName(), va.getStudent().getId());
                    
                    // Check if active token already exists
                    List<com.uhn.pmb.entity.ExamToken> existingTokens = tokenRepository.findAllByStudentId(va.getStudent().getId());
                    
                    com.uhn.pmb.entity.ExamToken tokenToUse;
                    com.uhn.pmb.entity.ExamToken activeToken = existingTokens.stream()
                            .filter(com.uhn.pmb.entity.ExamToken::isActive)
                            .findFirst()
                            .orElse(null);
                    
                    if (activeToken != null) {
                        log.info("✅ [SIMULATE-TOKEN-GEN] Active token already exists, reusing: {}", 
                                activeToken.getTokenValue());
                        tokenToUse = activeToken;
                    } else {
                        log.info("🎫 [SIMULATE-TOKEN-GEN] No active token found, generating new one for student {} (ID: {})...", 
                                va.getStudent().getFullName(), va.getStudent().getId());
                        
                        tokenToUse = examTokenService.generateToken(
                                va.getStudent().getId(),
                                va.getAdmissionForm().getId(),
                                120 // 2 jam expiration
                        );
                    }
                    
                    if (tokenToUse != null) {
                        log.info("✅ [SIMULATE-TOKEN-GEN SUCCESS] Token: {} for student: {}", 
                                tokenToUse.getTokenValue(), va.getStudent().getFullName());
                        
                        // ✅ Sync token to FormValidation for dashboard display
                        validation.setExamToken(tokenToUse.getTokenValue());
                        formValidationRepository.save(validation);
                        log.info("✅ [SYNC] Exam token synced to FormValidation record");
                    } else {
                        log.error("❌ [SIMULATE-TOKEN-GEN] Token is null");
                    }
                } catch (Exception tokenGenError) {
                    log.error("❌ [SIMULATE-TOKEN-GEN FAILED] Error generating token: {}", 
                            tokenGenError.getMessage(), tokenGenError);
                    // Print full stack trace for debugging
                    tokenGenError.printStackTrace();
                    // Don't fail the simulation if token generation fails
                }
                
            } else {
                log.warn("⚠️ [SIMULATE] FormValidation not found for AdmissionForm ID: {}", 
                        va.getAdmissionForm().getId());
                log.warn("⚠️ [SIMULATE] Student: {}, Form: {}", 
                        va.getStudent().getFullName(), va.getAdmissionForm().getId());
            }
        } catch (Exception e) {
            log.error("❌ [SIMULATE] Error updating FormValidation after payment simulation: {}", e.getMessage(), e);
            e.printStackTrace();
            // Don't throw - allow payment to succeed even if FormValidation update fails
        }
    }

    /**
     * ✅ NEW: Manual endpoint to trigger token generation (for debugging)
     * POST /api/camaba/trigger-token-generation
     * 
     * Usage: Admin can use this to manually trigger token generation if auto-generation failed
     */
    @PostMapping("/trigger-token-generation")
    @PreAuthorize("hasRole('CAMABA')")
    public ResponseEntity<?> triggerTokenGeneration() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            
            // Get latest admission form
            List<FormValidation> validations = formValidationRepository.findAll().stream()
                    .filter(fv -> fv.getStudent().getId().equals(student.getId()))
                    .toList();

            if (validations.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Tidak ada formulir pendaftaran");
                        }});
            }

            FormValidation validation = validations.get(0);
            AdmissionForm form = validation.getAdmissionForm();

            // ✅ SYNC: Check RegistrationStatus first, update FormValidation if needed
            log.info("🔄 [TRIGGER] Checking RegistrationStatus for payment sync...");
            Optional<RegistrationStatus> paymentStatusOpt = registrationStatusRepository
                    .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_BRIVA);
            
            if (paymentStatusOpt.isPresent() && 
                    paymentStatusOpt.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI) {
                // RegistrationStatus shows SELESAI, sync to FormValidation
                log.info("✅ [TRIGGER] RegistrationStatus.PAYMENT_BRIVA = SELESAI, auto-updating FormValidation...");
                validation.setPaymentStatus(FormValidation.PaymentStatus.PAID);
                validation.setPaymentDate(LocalDateTime.now());
                validation.setUpdatedAt(LocalDateTime.now());
                formValidationRepository.save(validation);
                log.info("✅ [TRIGGER] FormValidation synced to PAID");
            } else if (!FormValidation.PaymentStatus.PAID.equals(validation.getPaymentStatus())) {
                // Both RegistrationStatus and FormValidation show payment not completed
                log.warn("⚠️ [TRIGGER] Payment still PENDING. RegistrationStatus: {}, FormValidation: {}", 
                    paymentStatusOpt.isPresent() ? paymentStatusOpt.get().getStatus() : "NOT_FOUND",
                    validation.getPaymentStatus());
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Pembayaran belum selesai. Status: " + validation.getPaymentStatus());
                        }});
            }

            log.info("🎫 [MANUAL-TRIGGER] Generating token for student {} (checking existing first)...", student.getFullName());

            // Check if token already exists and is active
            List<com.uhn.pmb.entity.ExamToken> existingTokens = tokenRepository.findAllByStudentId(student.getId());
            com.uhn.pmb.entity.ExamToken activeToken = existingTokens.stream()
                    .filter(com.uhn.pmb.entity.ExamToken::isActive)
                    .findFirst()
                    .orElse(null);
            
            if (activeToken != null) {
                log.info("✅ [MANUAL-TRIGGER] Active token already exists: {}", activeToken.getTokenValue());
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", true);
                    put("message", "Token sudah tersedia");
                    put("tokenValue", activeToken.getTokenValue());
                    put("expiresAt", activeToken.getExpiresAt());
                }});
            }

            // Generate new token
            log.info("🎫 [MANUAL-TRIGGER] Generating NEW token for student {}...", student.getFullName());
            com.uhn.pmb.entity.ExamToken newToken = examTokenService.generateToken(
                    student.getId(),
                    form.getId(),
                    120 // 2 jam
            );

            if (newToken != null) {
                log.info("✅ [MANUAL-TRIGGER] Token generated successfully: {}", newToken.getTokenValue());
                
                // Sync to FormValidation
                validation.setExamToken(newToken.getTokenValue());
                formValidationRepository.save(validation);
                
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("success", true);
                    put("message", "Token berhasil di-generate");
                    put("tokenValue", newToken.getTokenValue());
                    put("expiresAt", newToken.getExpiresAt());
                }});
            } else {
                log.error("❌ [MANUAL-TRIGGER] Token generation returned null");
                return ResponseEntity.status(500)
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Gagal generate token (service returned null)");
                        }});
            }

        } catch (Exception e) {
            log.error("❌ [MANUAL-TRIGGER] Error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Error: " + e.getMessage());
                    }});
        }
    }

    /**
     * ===== CUSTOMER SERVICE MESSAGING ENDPOINTS =====
     */

    /**
     * ✅ NEW: Mark Exam as Started (PENDING status)
     * POST /api/camaba/exam/start
     * 
     * Called when student enters ujian.html page
     * Updates registration status PSYCHO_EXAM to MENUNGGU_VERIFIKASI
     */
    @PostMapping("/exam/start")
    @PreAuthorize("hasRole('CAMABA')")
    public ResponseEntity<?> markExamAsStarted() {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get or create registration status for PSYCHO_EXAM
            Optional<RegistrationStatus> statusOpt = registrationStatusRepository
                    .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PSYCHO_EXAM);
            
            RegistrationStatus psychoStatus;
            if (statusOpt.isEmpty()) {
                // Create new PSYCHO_EXAM status as MENUNGGU_VERIFIKASI
                psychoStatus = RegistrationStatus.builder()
                        .user(user)
                        .stage(RegistrationStatus.RegistrationStage.PSYCHO_EXAM)
                        .status(RegistrationStatus.RegistrationStatus_Enum.MENUNGGU_VERIFIKASI)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                psychoStatus = registrationStatusRepository.save(psychoStatus);
                log.info("✅ Created PSYCHO_EXAM registration status for user {}", user.getId());
            } else {
                // Update existing status to MENUNGGU_VERIFIKASI if not already SELESAI
                psychoStatus = statusOpt.get();
                if (!psychoStatus.getStatus().equals(RegistrationStatus.RegistrationStatus_Enum.SELESAI)) {
                    psychoStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.MENUNGGU_VERIFIKASI);
                    psychoStatus.setUpdatedAt(LocalDateTime.now());
                    psychoStatus = registrationStatusRepository.save(psychoStatus);
                    log.info("✅ Updated PSYCHO_EXAM status to MENUNGGU_VERIFIKASI for user {}", user.getId());
                }
            }
            
            // Extract status to variable to avoid effectively final issue in anonymous class
            final String statusText = psychoStatus.getStatus().toString();
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Exam marked as started");
                put("stage", "PSYCHO_EXAM");
                put("status", statusText);
            }});
            
        } catch (Exception e) {
            log.error("❌ Error marking exam as started: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Error: " + e.getMessage());
                    }});
        }
    }

    /**
     * ✅ NEW: Submit Exam Results After Completing GForm
     * POST /api/camaba/exam/submit-results
     * 
     * Student submits:
     * - Exam token (to verify it matches generated token)
     * - GForm score
     * - Proof photo of exam completion
     * 
     * Validates token match to prevent cheating
     */
    @PostMapping("/exam/submit-results")
    @PreAuthorize("hasRole('CAMABA')")
    public ResponseEntity<?> submitExamResults(
            @RequestParam String examToken,
            @RequestParam Double gformScore,
            @RequestParam(required = false) MultipartFile proofPhoto) {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            
            // Validate score
            if (gformScore < 0 || gformScore > 100) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Nilai harus antara 0-100");
                        }});
            }
            
            // Get FormValidation with exam token
            Optional<FormValidation> validationOpt = formValidationRepository
                    .findByStudentId(student.getId());
            
            if (validationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Data formulir tidak ditemukan");
                        }});
            }
            
            FormValidation validation = validationOpt.get();
            String generatedToken = validation.getExamToken();
            
            // Check if tokens match (case-insensitive)
            boolean tokenValid = (generatedToken != null && 
                    generatedToken.equalsIgnoreCase(examToken.trim()));
            
            log.info("🔐 [EXAM-SUBMISSION] Student {} submitted token. Generated: {}, Submitted: {}, Valid: {}", 
                    student.getId(), generatedToken, examToken, tokenValid);
            
            // Upload proof photo if provided
            String proofPhotoPath = null;
            if (proofPhoto != null && !proofPhoto.isEmpty()) {
                try {
                    String uploadsDir = "uploads/exam-proofs/";
                    new File(uploadsDir).mkdirs();
                    
                    String filename = "exam-proof-" + student.getId() + "-" + 
                            System.currentTimeMillis() + "-" + 
                            UUID.randomUUID().toString() + ".jpg";
                    
                    Path path = Paths.get(uploadsDir, filename);
                    Files.write(path, proofPhoto.getBytes());
                    
                    proofPhotoPath = "/" + uploadsDir + filename;
                    log.info("✅ Proof photo uploaded: {}", proofPhotoPath);
                    
                } catch (Exception photoError) {
                    log.warn("⚠️ Failed to upload proof photo: {}", photoError.getMessage());
                    // Don't fail submission if photo upload fails
                }
            }
            
            // Get or create Exam record for this student
            Optional<Exam> examOpt = examRepository.findByStudent_Id(student.getId());
            
            Exam exam;
            if (examOpt.isPresent()) {
                exam = examOpt.get();
            } else {
                // Get student's registration period (from admission form)
                List<AdmissionForm> admissionForms = admissionFormRepository.findByStudent_Id(student.getId());
                
                if (admissionForms.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new HashMap<String, Object>() {{
                                put("success", false);
                                put("message", "Data pendaftaran siswa tidak ditemukan");
                            }});
                }
                
                // Get the first admission form (or latest if multiple)
                AdmissionForm admissionForm = admissionForms.get(0);
                RegistrationPeriod period = admissionForm.getPeriod();
                
                if (period == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new HashMap<String, Object>() {{
                                put("success", false);
                                put("message", "Periode ujian tidak ditemukan");
                            }});
                }
                
                // Generate unique exam number
                String examNumber = generateExamNumber();
                
                // Create new Exam record with required fields
                exam = Exam.builder()
                        .examNumber(examNumber)
                        .student(student)
                        .period(period)
                        .status(Exam.ExamStatus.PENDING)
                        .startedAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();
                exam = examRepository.save(exam);
                log.info("✨ New exam created for student {}: examNumber={}", student.getId(), examNumber);
            }
            
            // Get or create ExamResult
            Optional<ExamResult> resultOpt = examResultRepository.findByExam_Id(exam.getId());
            
            ExamResult result;
            if (resultOpt.isPresent()) {
                result = resultOpt.get();
                log.info("📝 Updating existing exam result for student {}", student.getId());
            } else {
                result = ExamResult.builder()
                        .exam(exam)
                        .student(student)
                        .status(ExamResult.ResultStatus.PENDING)
                        .examValidationStatus(ExamResult.ExamValidationStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                log.info("✨ Creating new exam result for student {}", student.getId());
            }
            
            // Update with submission data
            result.setStudentInputToken(examToken.trim().toUpperCase());
            result.setGeneratedToken(generatedToken);
            result.setGformScore(gformScore);
            result.setProofPhotoPath(proofPhotoPath);
            result.setTokenValidated(tokenValid);
            result.setSubmissionDate(LocalDateTime.now());
            result.setScore(gformScore);
            result.setStatus(gformScore >= 70 ? ExamResult.ResultStatus.PASSED : ExamResult.ResultStatus.FAILED);
            
            // If token doesn't match, mark as rejected immediately (fraud alert)
            if (!tokenValid) {
                result.setExamValidationStatus(ExamResult.ExamValidationStatus.REJECTED);
                result.setAdminNotes("🚨 TOKEN MISMATCH DETECTED - Possible fraud attempt");
                result.setExamValidatedAt(LocalDateTime.now());
                log.warn("❌ FRAUD ALERT: Token mismatch for student {}: {} vs {}", 
                        student.getId(), generatedToken, examToken);
            } else {
                result.setExamValidationStatus(ExamResult.ExamValidationStatus.PENDING);
                log.info("✅ Token validated successfully for student {}", student.getId());
            }
            
            result.setUpdatedAt(LocalDateTime.now());
            result = examResultRepository.save(result);
            
            // Update Exam status to COMPLETED when results are submitted
            exam.setStatus(Exam.ExamStatus.COMPLETED);
            exam.setCompletedAt(LocalDateTime.now());
            exam.setUpdatedAt(LocalDateTime.now());
            exam = examRepository.save(exam);
            log.info("✅ Exam marked as COMPLETED for student {}", student.getId());
            
            // ✅ NEW: Update RegistrationStatus for PSYCHO_EXAM stage to SELESAI
            // This is what the dashboard polls to show the updated status
            try {
                Optional<RegistrationStatus> regStatusOpt = registrationStatusRepository
                        .findByUserAndStage(user,
                                RegistrationStatus.RegistrationStage.PSYCHO_EXAM);
                
                if (regStatusOpt.isPresent()) {
                    RegistrationStatus psychoStatus = regStatusOpt.get();
                    psychoStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                    psychoStatus.setSubmissionDate(LocalDateTime.now());
                    psychoStatus.setUpdatedAt(LocalDateTime.now());
                    registrationStatusRepository.save(psychoStatus);
                    log.info("✅ RegistrationStatus PSYCHO_EXAM updated to SELESAI for student {}", student.getId());
                } else {
                    log.warn("⚠️ RegistrationStatus PSYCHO_EXAM not found for student {}", student.getId());
                }
            } catch (Exception regStatusError) {
                log.warn("⚠️ Failed to update RegistrationStatus: {}", regStatusError.getMessage());
                // Don't fail the submission if status update fails
            }
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", tokenValid ? 
                    "Hasil ujian berhasil disubmit dan menunggu validasi admin" : 
                    "⚠️ Hasil ujian disubmit tapi token tidak cocok - akan ditinjau admin");
            response.put("examResultId", result.getId());
            response.put("tokenValidated", tokenValid);
            response.put("score", gformScore);
            response.put("validationStatus", result.getExamValidationStatus().toString());
            response.put("submittedAt", LocalDateTime.now());
            
            log.info("📤 Exam results submitted by student {}: score={}, tokenValid={}", 
                    student.getId(), gformScore, tokenValid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error submitting exam results: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Gagal submit hasil ujian: " + e.getMessage());
                    }});
        }
    }

    /**
     * Mahasiswa mengirim pesan ke admin customer service
     * POST /api/camaba/messages/send-to-admin
     */
    @PostMapping("/messages/send-to-admin")
    public ResponseEntity<?> sendMessageToAdmin(@RequestBody SendMessageRequest request) {
        try {
            String senderEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User sender = userRepository.findByEmail(senderEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate message content FIRST
            if (request.getMessageContent() == null || request.getMessageContent().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Pesan tidak boleh kosong");
                        }});
            }
            
            String messageContent = request.getMessageContent().trim();
            if (messageContent.length() < 5) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Pesan minimal 5 karakter");
                        }});
            }
            
            // Get a CS admin (ADMIN_PUSAT or ADMIN_VALIDASI) - send to ONE recipient
            User recipient = null;
            
            // Try ADMIN_PUSAT first
            List<User> adminList = userRepository.findByRole(User.UserRole.ADMIN_PUSAT);
            if (adminList != null && !adminList.isEmpty()) {
                recipient = adminList.get(0);
            }
            
            // If no ADMIN_PUSAT, try ADMIN_VALIDASI
            if (recipient == null) {
                adminList = userRepository.findByRole(User.UserRole.ADMIN_VALIDASI);
                if (adminList != null && !adminList.isEmpty()) {
                    recipient = adminList.get(0);
                }
            }
            
            if (recipient == null) {
                log.warn("⚠️ No admin found to receive messages");
                return ResponseEntity.status(500)
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Maaf, tidak ada admin yang tersedia untuk menerima pesan. Hubungi admin secara langsung."); 
                        }});
            }
            
            // Create and save message (only 1 copy)
            AdminMessage message = AdminMessage.builder()
                    .sender(sender)
                    .recipient(recipient)
                    .messageContent(messageContent)
                    .messageType(request.getMessageType() != null ? request.getMessageType() : "QUESTION")
                    .status(AdminMessage.MessageStatus.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            adminMessageRepository.save(message);
            
            log.info("✅ Message sent from student {} to admin {}", senderEmail, recipient.getEmail());
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Pesan berhasil dikirim ke Admin Customer Service");
                put("messageId", message.getId());
                put("sentAt", message.getCreatedAt());
            }});
        } catch (Exception e) {
            log.error("❌ Error sending message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", e.getMessage());
                    }});
        }
    }

    /**
     * Mahasiswa melihat pesan-pesan mereka dengan admin (conversation)
     * GET /api/camaba/messages
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getStudentMessages() {
        try {
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get all messages for this conversation (1 copy each, no duplicates)
            List<AdminMessage> messages = adminMessageRepository.findConversationWith(user.getId());
            
            // Map to DTO to avoid lazy loading issues
            List<Map<String, Object>> responseMsgs = new ArrayList<>();
            
            for (AdminMessage msg : messages) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                
                // Add senderId and senderEmail
                Long senderId = msg.getSender() != null ? msg.getSender().getId() : null;
                String senderEmail = msg.getSender() != null ? msg.getSender().getEmail() : "Unknown";
                msgMap.put("senderId", senderId);
                msgMap.put("senderEmail", senderEmail);
                msgMap.put("senderName", senderEmail);
                msgMap.put("senderType", msg.getSender().getId().equals(user.getId()) ? "STUDENT" : "ADMIN");
                msgMap.put("senderRole", msg.getSender().getRole());
                
                // Add recipientId and recipientEmail
                Long recipientId = msg.getRecipient() != null ? msg.getRecipient().getId() : null;
                String recipientEmail = msg.getRecipient() != null ? msg.getRecipient().getEmail() : "Unknown";
                msgMap.put("recipientId", recipientId);
                msgMap.put("recipientEmail", recipientEmail);
                
                msgMap.put("messageContent", msg.getMessageContent());
                msgMap.put("messageType", msg.getMessageType());
                msgMap.put("status", msg.getStatus().toString());
                msgMap.put("createdAt", msg.getCreatedAt());
                msgMap.put("readAt", msg.getReadAt());
                
                responseMsgs.add(msgMap);
            }
            
            // Mark messages as read if they were for this student
            for (AdminMessage msg : messages) {
                if (msg.getRecipient().getId().equals(user.getId()) && 
                    msg.getStatus() == AdminMessage.MessageStatus.UNREAD) {
                    msg.setStatus(AdminMessage.MessageStatus.READ);
                    msg.setReadAt(LocalDateTime.now());
                    adminMessageRepository.save(msg);
                }
            }
            
            log.info("✅ Retrieved {} messages for student {}", responseMsgs.size(), userEmail);
            
            return ResponseEntity.ok(responseMsgs);
        } catch (Exception e) {
            log.error("❌ Error fetching messages: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Hitung jumlah pesan yang belum dibaca untuk student
     * GET /api/camaba/messages/unread-count
     */
    @GetMapping("/messages/unread-count")
    @PreAuthorize("permitAll()")  // Header component polls this even from public pages
    public ResponseEntity<?> getStudentUnreadCount() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{ put("unreadCount", 0); }});
            }
            String userEmail = auth.getName();
            var userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{ put("unreadCount", 0); }});
            }
            User student = userOpt.get();

            // Count unread messages where student is recipient
            long unreadCount = adminMessageRepository.findAll().stream()
                    .filter(msg -> msg.getRecipient().getId().equals(student.getId()) &&
                                   msg.getStatus() == AdminMessage.MessageStatus.UNREAD)
                    .count();

            log.info("✅ Unread count for student {}: {}", userEmail, unreadCount);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("unreadCount", unreadCount);
            }});
        } catch (Exception e) {
            log.error("❌ Error getting unread count: {}", e.getMessage());
            return ResponseEntity.ok(new HashMap<String, Object>() {{ put("unreadCount", 0); }});
        }
    }

    /**
     * Tandai semua pesan sebagai dibaca untuk student
     * POST /api/camaba/messages/mark-read
     */
    @PostMapping("/messages/mark-read")
    public ResponseEntity<?> markStudentMessagesAsRead() {
        try {
            String userEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User student = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Mark all unread messages from admin as read
            List<AdminMessage> unreadMessages = adminMessageRepository.findAll().stream()
                    .filter(msg -> msg.getRecipient().getId().equals(student.getId()) &&
                                   msg.getStatus() == AdminMessage.MessageStatus.UNREAD)
                    .collect(Collectors.toList());

            unreadMessages.forEach(msg -> {
                msg.setStatus(AdminMessage.MessageStatus.READ);
                msg.setReadAt(LocalDateTime.now());
                adminMessageRepository.save(msg);
            });

            log.info("✅ Marked {} messages as read for student {}", unreadMessages.size(), userEmail);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Semua pesan telah ditandai sebagai dibaca");
                put("markedCount", unreadMessages.size());
            }});
        } catch (Exception e) {
            log.error("❌ Error marking messages as read: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", e.getMessage());
                    }});
        }
    }

    // ✅ NEW: RE-ENROLLMENT ENDPOINTS

    /**
     * Mahasiswa submit daftar ulang dengan dokumen
     * POST /api/camaba/reenrollment/submit
     */
    @PostMapping("/reenrollment/submit")
    public ResponseEntity<?> submitReenrollment(
            @RequestParam String parentPhone,
            @RequestParam String parentEmail,
            @RequestParam String parentAddress,
            @RequestParam String permanentAddress,
            @RequestParam(defaultValue = "") String currentAddress,
            @RequestParam(defaultValue = "false") Boolean alumniFamily,
            @RequestParam(required = false) String alumniName,
            @RequestParam(required = false) String alumniRelation,
            @RequestParam Map<String, MultipartFile> documents) {
        try {
            // ✅ Get authenticated user
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            log.info("📝 [REENROLL-SUBMIT] Processing re-enrollment for student: {} (ID: {})", studentEmail, student.getId());

            // ✅ Get exam result if it exists (optional for some wave types)
            ExamResult examResult = null;
            try {
                Optional<Exam> examOpt = examRepository.findByStudent_Id(student.getId());
                if (examOpt.isPresent()) {
                    examResult = examResultRepository.findByExam_Id(examOpt.get().getId())
                            .filter(result -> result.getStatus() == ExamResult.ResultStatus.PASSED)
                            .orElse(null);
                }
            } catch (Exception e) {
                log.warn("⚠️ [REENROLL-SUBMIT] Could not fetch exam result: {}", e.getMessage());
            }

            // ✅ Check if already submitted (non-rejected)
            Optional<ReEnrollment> existing = reenrollmentRepository
                    .findAll()
                    .stream()
                    .filter(r -> r.getStudent().getId().equals(student.getId()))
                    .filter(r -> r.getStatus() != ReEnrollment.ReEnrollmentStatus.REJECTED)
                    .findFirst();

            if (existing.isPresent()) {
                log.warn("⚠️ [REENROLL-SUBMIT] Student already has submitted re-enrollment");
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", false,
                            "message", "Anda sudah melakukan daftar ulang sebelumnya"
                        ));
            }

            // ✅ Create upload directory
            String uploadDir = System.getProperty("user.dir") + File.separator + 
                    "uploads" + File.separator + "reenrollment";
            
            try {
                Files.createDirectories(Paths.get(uploadDir));
                log.info("✅ [REENROLL-SUBMIT] Upload directory ready: {}", uploadDir);
            } catch (IOException e) {
                log.error("❌ [REENROLL-SUBMIT] Failed to create upload directory: {}", e.getMessage());
                throw new RuntimeException("Gagal membuat direktori upload");
            }

            // ✅ Build ReEnrollment record with all data
            ReEnrollment reenrollment = ReEnrollment.builder()
                    .examResult(examResult)
                    .student(student)
                    .parentName(student.getFullName()) // Use student's name as fallback
                    .parentPhone(parentPhone)
                    .parentEmail(parentEmail)
                    .parentAddress(parentAddress)
                    .permanentAddress(permanentAddress)
                    .currentAddress(currentAddress.isEmpty() ? permanentAddress : currentAddress)
                    .alumniFamily(alumniFamily)
                    .alumniName(alumniName)
                    .alumniRelation(alumniRelation)
                    .status(ReEnrollment.ReEnrollmentStatus.SUBMITTED)
                    .submittedAt(LocalDateTime.now())
                    .documents(new ArrayList<>())
                    .build();

            // ✅ Save base record first
            reenrollment = reenrollmentRepository.save(reenrollment);
            log.info("✅ [REENROLL-SUBMIT] ReEnrollment saved, ID: {}", reenrollment.getId());

            // ✅ Process document uploads
            Map<String, String> documentPaths = new HashMap<>();
            
            for (Map.Entry<String, MultipartFile> entry : documents.entrySet()) {
                String key = entry.getKey(); // Format: "documents[PACTA_INTEGRITAS]"
                MultipartFile file = entry.getValue();

                if (file == null || file.isEmpty()) {
                    log.warn("⚠️ [REENROLL-SUBMIT] Empty file for key: {}", key);
                    continue;
                }

                // Extract document type from key
                String docTypeStr = key.replace("documents[", "").replace("]", "").toUpperCase();
                log.debug("   Processing document type: {}", docTypeStr);

                // Validate file
                if (file.getSize() > 5 * 1024 * 1024) {
                    log.warn("⚠️ [REENROLL-SUBMIT] File too large: {} ({} bytes)", file.getOriginalFilename(), file.getSize());
                    continue;
                }

                try {
                    // Validate document type exists in enum
                    ReEnrollmentDocument.DocumentType docType = 
                            ReEnrollmentDocument.DocumentType.valueOf(docTypeStr);

                    // Save file to disk
                    String filename = UUID.randomUUID().toString() + "_" + 
                            (file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "file");
                    String filepath = uploadDir + File.separator + filename;
                    
                    Files.write(Paths.get(filepath), file.getBytes());
                    log.info("✅ [REENROLL-SUBMIT] File saved: {} -> {}", file.getOriginalFilename(), filepath);

                    // Store in map for database update
                    documentPaths.put(docTypeStr, filepath);

                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ [REENROLL-SUBMIT] Invalid document type: {} - {}", docTypeStr, e.getMessage());
                    // Continue with next document instead of failing
                } catch (IOException e) {
                    log.error("❌ [REENROLL-SUBMIT] Failed to write file: {}", e.getMessage());
                    // Continue instead of failing entire submission
                }
            }

            // ✅ Update reenrollment with document file paths
            if (!documentPaths.isEmpty()) {
                if (documentPaths.containsKey("PAKTA_INTEGRITAS")) {
                    reenrollment.setPaktaIntegritasFile(documentPaths.get("PAKTA_INTEGRITAS"));
                }
                if (documentPaths.containsKey("IJAZAH")) {
                    reenrollment.setIjazahFile(documentPaths.get("IJAZAH"));
                }
                if (documentPaths.containsKey("PASPHOTO")) {
                    reenrollment.setPasphotoFile(documentPaths.get("PASPHOTO"));
                }
                if (documentPaths.containsKey("KARTU_KELUARGA")) {
                    reenrollment.setKartuKeluargaFile(documentPaths.get("KARTU_KELUARGA"));
                }
                if (documentPaths.containsKey("KARTU_TANDA_PENDUDUK")) {
                    reenrollment.setKtpFile(documentPaths.get("KARTU_TANDA_PENDUDUK"));
                }
                if (documentPaths.containsKey("KETERANGAN_BEBAS_NARKOBA")) {
                    reenrollment.setSuratBebasNarkobaFile(documentPaths.get("KETERANGAN_BEBAS_NARKOBA"));
                }
                if (documentPaths.containsKey("SKCK")) {
                    reenrollment.setSkckFile(documentPaths.get("SKCK"));
                }

                reenrollment = reenrollmentRepository.save(reenrollment);
                log.info("✅ [REENROLL-SUBMIT] Document paths saved: {} files", documentPaths.size());
            }

            // ✅ Update REGISTRATION_STAGES
            try {
                Optional<RegistrationStatus> daftarUlangStatus = registrationStatusRepository
                        .findByUserAndStage(user, RegistrationStatus.RegistrationStage.DAFTAR_ULANG);

                if (daftarUlangStatus.isPresent()) {
                    RegistrationStatus status = daftarUlangStatus.get();
                    status.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                    status.setUpdatedAt(LocalDateTime.now());
                    registrationStatusRepository.save(status);
                    log.info("✅ [REENROLL-SUBMIT] DAFTAR_ULANG marked SELESAI");
                } else {
                    log.warn("⚠️ [REENROLL-SUBMIT] DAFTAR_ULANG not found in REGISTRATION_STAGES");
                }
            } catch (Exception e) {
                log.error("❌ [REENROLL-SUBMIT] Failed to update REGISTRATION_STAGES: {}", e.getMessage());
            }

            log.info("✅ [REENROLL-SUBMIT] Submission complete - Re-enrollment ID: {}, Documents: {}", 
                    reenrollment.getId(), documentPaths.size());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Daftar ulang berhasil dikirim!",
                "reenrollmentId", reenrollment.getId(),
                "status", reenrollment.getStatus().toString(),
                "documentsCount", documentPaths.size()
            ));

        } catch (Exception e) {
            log.error("❌ [REENROLL-SUBMIT] ERROR: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Gagal memproses daftar ulang: " + e.getMessage(),
                        "error", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * Mahasiswa cek status daftar ulang
     * GET /api/camaba/reenrollment/status
     */
    @GetMapping("/reenrollment/status")
    public ResponseEntity<?> getReerollmentStatus() {
        try {
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Optional<ReEnrollment> reenrollment = reenrollmentRepository
                    .findAll()
                    .stream()
                    .filter(r -> r.getStudent().getId().equals(student.getId()))
                    .max(Comparator.comparing(ReEnrollment::getSubmittedAt));

            if (reenrollment.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("status", "NOT_STARTED");
                    put("message", "Belum ada daftar ulang");
                }});
            }

            ReEnrollment re = reenrollment.get();
            int totalDocs = re.getDocuments().size();
            int approvedDocs = (int) re.getDocuments().stream()
                    .filter(d -> d.getValidationStatus() == ReEnrollmentDocument.ValidationStatus.APPROVED)
                    .count();

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("status", re.getStatus().toString());
                put("submittedAt", re.getSubmittedAt());
                put("validatedAt", re.getValidatedAt());
                put("totalDocuments", totalDocs);
                put("approvedDocuments", approvedDocs);
                put("validationNotes", re.getValidationNotes());
            }});

        } catch (Exception e) {
            log.error("❌ Error getting re-enrollment status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * ✅ NEW DEDICATED ENDPOINT: Mark DAFTAR_ULANG stage as complete
     * POST /api/camaba/reenrollment/complete
     * 
     * Similar to /registration-status/{stage}/complete for other stages
     * Creates or updates DAFTAR_ULANG stage to SELESAI when daftar ulang is submitted
     * 
     * This is the definitive endpoint to update daftar ulang status
     * Called after successful reenrollment submission
     */
    @PostMapping("/reenrollment/complete")
    public ResponseEntity<?> completeReerollment(
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("📝 [REENROLL-COMPLETE] Marking DAFTAR_ULANG as SELESAI");
            log.info("═══════════════════════════════════════════════════════════");
            
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            log.info("👤 Student: {} (ID: {})", user.getEmail(), student.getId());

            // ✅ KEY: Get or CREATE DAFTAR_ULANG stage
            // This is the CRITICAL FIX - use getOrCreateStatus to ensure stage exists
            RegistrationStatus daftarUlangStatus = registrationStatusService.getOrCreateStatus(
                    user,
                    RegistrationStatus.RegistrationStage.DAFTAR_ULANG
            );

            log.info("📍 Found/Created DAFTAR_ULANG status - ID: {}, Current status: {}", 
                    daftarUlangStatus.getId(), daftarUlangStatus.getStatus());

            // ✅ Mark as SELESAI (completed)
            daftarUlangStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
            daftarUlangStatus.setSubmissionDate(LocalDateTime.now());
            daftarUlangStatus.setUpdatedAt(LocalDateTime.now());
            
            // Add context info about submission
            if (request != null && request.containsKey("submittedAt")) {
                daftarUlangStatus.setDataJson("Daftar ulang submitted at: " + request.get("submittedAt"));
            }

            RegistrationStatus savedStatus = registrationStatusRepository.save(daftarUlangStatus);
            log.info("✅ [REENROLL-COMPLETE] DAFTAR_ULANG marked SELESAI");
            log.info("   - Status ID: {}", savedStatus.getId());
            log.info("   - Status: {}", savedStatus.getStatus());
            log.info("   - Submission Date: {}", savedStatus.getSubmissionDate());

            // ✅ Build response matching pattern of other endpoints
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Daftar ulang berhasil ditandai sebagai selesai");
            response.put("stage", "DAFTAR_ULANG");
            response.put("status", savedStatus.getStatus().toString());
            response.put("statusId", savedStatus.getId());
            response.put("completedAt", LocalDateTime.now());
            response.put("editDeadline", savedStatus.getEditDeadline());

            log.info("═══════════════════════════════════════════════════════════");
            log.info("✅ SUCCESS: DAFTAR_ULANG stage update complete!");
            log.info("═══════════════════════════════════════════════════════════");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ [REENROLL-COMPLETE] ERROR: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "Gagal menandai daftar ulang sebagai selesai: " + e.getMessage(),
                        "error", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * ✅ NEW: Helper method to convert absolute file paths to relative URLs
     * Converts: D:/all code/tugasakhir/uploads/reenrollment/filename.png
     * To: /uploads/reenrollment/filename.png
     * Or: /api/files/uploads/reenrollment/filename.png (if API needs to serve)
     */
    private String convertToUrl(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return null;
        }
        
        try {
            // Remove Windows drive letters and backslashes
            String normalized = absolutePath.replace("\\", "/");
            
            // ✅ FIX: Check for both "/uploads/" (with leading slash) and "uploads/" (without)
            int uploadsIndex = normalized.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                return normalized.substring(uploadsIndex);  // e.g., /uploads/reenrollment/file.png
            }
            
            // ✅ NEW: Also check for "uploads/" without leading slash
            uploadsIndex = normalized.indexOf("uploads/");
            if (uploadsIndex >= 0) {
                return "/" + normalized.substring(uploadsIndex);  // e.g., /uploads/reenrollment/file.png
            }
            
            // If no uploads path found, return original path with leading slash
            log.warn("⚠️ convertToUrl: No 'uploads/' found in path: {}", absolutePath);
            return "/" + absolutePath;
        } catch (Exception e) {
            log.warn("⚠️ Error converting path to URL: {}", e.getMessage());
            return "/" + absolutePath;  // Return with leading slash
        }
    }

    /**
     * ===== NEW: Get reenrollment data for viewing/editing =====
     * GET /api/camaba/reenrollment
     * Returns the latest reenrollment record for authenticated student
     */
    @GetMapping("/reenrollment")
    public ResponseEntity<?> getReerollmentData() {
        try {
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Get latest reenrollment for this student
            Optional<ReEnrollment> reenrollmentOpt = reenrollmentRepository
                    .findAll()
                    .stream()
                    .filter(r -> r.getStudent().getId().equals(student.getId()))
                    .max(Comparator.comparing(ReEnrollment::getSubmittedAt));

            if (reenrollmentOpt.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("exists", false);
                    put("message", "Belum ada data daftar ulang");
                }});
            }

            ReEnrollment reenrollment = reenrollmentOpt.get();
            
            // ✅ FORCE LOAD: Eagerly load documents collection from database
            // This ensures lazy-loaded collection is initialized before response
            if (reenrollment.getDocuments() == null) {
                log.warn("⚠️ Documents collection is null, initializing...");
                reenrollment.setDocuments(new ArrayList<>());
            } else {
                // Force load collection by calling .size() to initialize lazy proxy
                int docCount = reenrollment.getDocuments().size();
                log.info("✅ Documents collection loaded: {} documents", docCount);
            }
            
            // ✅ Build file information from ReEnrollmentDocument collection
            Map<String, Object> files = new HashMap<>();
            
            // ✅ READ FROM COLLECTION (not from old database columns)
            if (reenrollment.getDocuments() != null && !reenrollment.getDocuments().isEmpty()) {
                for (ReEnrollmentDocument doc : reenrollment.getDocuments()) {
                    String docType = doc.getDocumentType().toString();
                    String fileUrl = convertToUrl(doc.getFilePath());
                    files.put(docType, fileUrl);
                    log.info("✅ Document {} for re-enrollment {}: {}", docType, reenrollment.getId(), fileUrl);
                }
            }
            
            // ✅ FALLBACK: Read from old database columns if migration not complete
            // This ensures backward compatibility during transition
            if (reenrollment.getPaktaIntegritasFile() != null && !files.containsKey("PAKTA_INTEGRITAS")) {
                files.put("PAKTA_INTEGRITAS", convertToUrl(reenrollment.getPaktaIntegritasFile()));
            }
            if (reenrollment.getIjazahFile() != null && !files.containsKey("IJAZAH")) {
                files.put("IJAZAH", convertToUrl(reenrollment.getIjazahFile()));
            }
            if (reenrollment.getPasphotoFile() != null && !files.containsKey("PASPHOTO")) {
                files.put("PASPHOTO", convertToUrl(reenrollment.getPasphotoFile()));
            }
            if (reenrollment.getKartuKeluargaFile() != null && !files.containsKey("KARTU_KELUARGA")) {
                files.put("KARTU_KELUARGA", convertToUrl(reenrollment.getKartuKeluargaFile()));
            }
            if (reenrollment.getKtpFile() != null && !files.containsKey("KARTU_TANDA_PENDUDUK")) {
                files.put("KARTU_TANDA_PENDUDUK", convertToUrl(reenrollment.getKtpFile()));
            }
            if (reenrollment.getSuratBebasNarkobaFile() != null && !files.containsKey("KETERANGAN_BEBAS_NARKOBA")) {
                files.put("KETERANGAN_BEBAS_NARKOBA", convertToUrl(reenrollment.getSuratBebasNarkobaFile()));
            }
            if (reenrollment.getSkckFile() != null && !files.containsKey("SKCK")) {
                files.put("SKCK", convertToUrl(reenrollment.getSkckFile()));
            }
            
            // Return full reenrollment data for display/edit
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("exists", true);
                put("id", reenrollment.getId());
                put("parentName", reenrollment.getParentName());
                put("parentPhone", reenrollment.getParentPhone());
                put("parentEmail", reenrollment.getParentEmail());
                put("parentAddress", reenrollment.getParentAddress());
                put("permanentAddress", reenrollment.getPermanentAddress());
                put("currentAddress", reenrollment.getCurrentAddress());
                put("alumniFamily", reenrollment.getAlumniFamily());
                put("alumniName", reenrollment.getAlumniName());
                put("alumniRelation", reenrollment.getAlumniRelation());
                put("status", reenrollment.getStatus().toString());
                put("submittedAt", reenrollment.getSubmittedAt());
                put("validatedAt", reenrollment.getValidatedAt());
                put("validationNotes", reenrollment.getValidationNotes());
                put("documents", files);  // ✅ Add file URLs
                put("documentsCount", files.size());
            }});

        } catch (Exception e) {
            log.error("❌ Error getting re-enrollment data: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * ===== NEW: Update reenrollment data (edit) =====
     * PUT /api/camaba/reenrollment/{id}
     * Allows student to edit their reenrollment data before submission
     * ✅ NOW SUPPORTS: multipart/form-data with text fields AND file uploads
     */
    @PutMapping(value = "/reenrollment/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CAMABA')")  // ✅ ADD THIS - SAME AS ADMISSION FORM
    public ResponseEntity<?> updateReerollmentData(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            ReEnrollment reenrollment = reenrollmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Re-enrollment not found"));

            // Verify ownership
            if (!reenrollment.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.status(403)
                        .body(new HashMap<String, Object>() {{
                            put("error", "Unauthorized");
                        }});
            }

            // Only allow edit if status is INCOMPLETE or SUBMITTED
            if (reenrollment.getStatus() == ReEnrollment.ReEnrollmentStatus.VALIDATED ||
                reenrollment.getStatus() == ReEnrollment.ReEnrollmentStatus.REJECTED) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("error", "Tidak dapat mengedit data yang sudah divalidasi");
                        }});
            }

            // ✅ Parse multipart request (same pattern as /admission-form)
            if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                org.springframework.web.multipart.MultipartHttpServletRequest multipart = 
                    (org.springframework.web.multipart.MultipartHttpServletRequest) request;
                
                // ===== TEXT FIELDS =====
                String parentName = multipart.getParameter("parentName");
                String parentPhone = multipart.getParameter("parentPhone");
                String parentEmail = multipart.getParameter("parentEmail");
                String parentAddress = multipart.getParameter("parentAddress");
                String permanentAddress = multipart.getParameter("permanentAddress");
                String currentAddress = multipart.getParameter("currentAddress");
                String alumniFamily = multipart.getParameter("alumniFamily");
                String alumniRelation = multipart.getParameter("alumniRelation");
                String alumniName = multipart.getParameter("alumniName");
                
                // Update text fields
                if (parentName != null && !parentName.isEmpty()) reenrollment.setParentName(parentName);
                if (parentPhone != null && !parentPhone.isEmpty()) reenrollment.setParentPhone(parentPhone);
                if (parentEmail != null && !parentEmail.isEmpty()) reenrollment.setParentEmail(parentEmail);
                if (parentAddress != null && !parentAddress.isEmpty()) reenrollment.setParentAddress(parentAddress);
                if (permanentAddress != null && !permanentAddress.isEmpty()) reenrollment.setPermanentAddress(permanentAddress);
                if (currentAddress != null && !currentAddress.isEmpty()) reenrollment.setCurrentAddress(currentAddress);
                if (alumniFamily != null && !alumniFamily.isEmpty()) reenrollment.setAlumniFamily(Boolean.parseBoolean(alumniFamily));
                if (alumniRelation != null && !alumniRelation.isEmpty()) reenrollment.setAlumniRelation(alumniRelation);
                if (alumniName != null && !alumniName.isEmpty()) reenrollment.setAlumniName(alumniName);
                
                // ===== FILE UPLOADS =====
                // Create uploads directory if not exists
                String uploadsPath = "uploads/reenrollment/" + student.getId();
                Files.createDirectories(Paths.get(uploadsPath));
                
                // Handle document uploads - SIMPLIFIED LOGIC
                String[] docTypes = {"PAKTA_INTEGRITAS", "IJAZAH", "PASPHOTO", "KARTU_KELUARGA", 
                                    "KARTU_TANDA_PENDUDUK", "KETERANGAN_BEBAS_NARKOBA", "SKCK"};
                
                for (String docType : docTypes) {
                    String paramKey = "documents[" + docType + "]";
                    MultipartFile file = multipart.getFile(paramKey);
                    
                    if (file != null && !file.isEmpty()) {
                        // Save file to disk
                        String fileName = docType + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                        Path filePath = Paths.get(uploadsPath, fileName);
                        Files.write(filePath, file.getBytes());
                        
                        // SIMPLIFIED: Remove old document if exists, then create new one
                        reenrollment.getDocuments().removeIf(d -> d.getDocumentType().toString().equals(docType));
                        
                        // Create NEW document record
                        ReEnrollmentDocument doc = ReEnrollmentDocument.builder()
                            .reenrollment(reenrollment)
                            .documentType(ReEnrollmentDocument.DocumentType.valueOf(docType))
                            .filePath("uploads/reenrollment/" + student.getId() + "/" + fileName)
                            .originalFilename(file.getOriginalFilename())
                            .fileSize(file.getSize())
                            .fileMimeType(file.getContentType())
                            .uploadStatus(ReEnrollmentDocument.UploadStatus.COMPLETED)
                            .uploadedAt(LocalDateTime.now())
                            .validationStatus(ReEnrollmentDocument.ValidationStatus.PENDING)
                            .build();
                        
                        // Add to reenrollment documents
                        reenrollment.getDocuments().add(doc);
                        
                        log.info("✅ Document uploaded {} for reenrollment {}: {}", docType, id, fileName);
                    }
                }
            }

            reenrollment = reenrollmentRepository.save(reenrollment);

            final ReEnrollment savedReenrollment = reenrollment;
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "✅ Data daftar ulang berhasil diperbarui dengan dokumen");
                put("id", savedReenrollment.getId());
                put("status", savedReenrollment.getStatus().toString());
            }});

        } catch (Exception e) {
            final String errorMsg = e.getMessage();
            log.error("❌ Error updating re-enrollment data: {}", errorMsg);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("error", errorMsg);
                    }});
        }
    }

    /**
     * Mahasiswa lihat dokumen daftar ulang mereka
     * GET /api/camaba/reenrollment/{id}/documents
     */
    @GetMapping("/reenrollment/{id}/documents")
    public ResponseEntity<?> getReerollmentDocuments(@PathVariable Long id) {
        try {
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            ReEnrollment reenrollment = reenrollmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Re-enrollment not found"));

            // Verify ownership
            if (!reenrollment.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.status(403)
                        .body(new HashMap<String, Object>() {{
                            put("error", "Unauthorized");
                        }});
            }

            List<Map<String, Object>> docs = new ArrayList<>();
            for (ReEnrollmentDocument doc : reenrollment.getDocuments()) {
                docs.add(new HashMap<String, Object>() {{
                    put("id", doc.getId());
                    put("documentType", doc.getDocumentType().toString());
                    put("displayName", doc.getDocumentType().getDisplayName());
                    put("fileName", doc.getOriginalFilename());
                    put("uploadedAt", doc.getUploadedAt());
                    put("validationStatus", doc.getValidationStatus().toString());
                    put("adminNotes", doc.getAdminNotes());
                }});
            }

            return ResponseEntity.ok(docs);

        } catch (Exception e) {
            log.error("❌ Error getting documents: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Student view hasil akhir (final results)
     * GET /api/camaba/hasil-akhir
     * Returns BRIVA number, nomor registrasi, and status
     */
    @GetMapping("/hasil-akhir")
    public ResponseEntity<?> getHasilAkhir() {
        try {
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            Optional<HasilAkhir> hasilAkhirOpt = hasilAkhirService.getHasilAkhirByStudentId(student.getId());

            if (hasilAkhirOpt.isEmpty()) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("exists", false);
                    put("message", "Hasil akhir belum tersedia");
                }});
            }

            HasilAkhir hasilAkhir = hasilAkhirOpt.get();
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("exists", true);
                put("id", hasilAkhir.getId());
                put("brivaNumber", hasilAkhir.getBrivaNumber());
                put("brivaAmount", hasilAkhir.getBrivaAmount());
                put("nomorRegistrasi", hasilAkhir.getNomorRegistrasi());
                put("status", hasilAkhir.getStatus().toString());
                put("npmSementaraFile", hasilAkhir.getNpmSementaraFile());
                put("ktmSementaraFile", hasilAkhir.getKtmSementaraFile());
                put("createdAt", hasilAkhir.getCreatedAt());
                put("updatedAt", hasilAkhir.getUpdatedAt());
            }});

        } catch (Exception e) {
            log.error("❌ Error getting hasil akhir: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * ====== EXAM TOKEN GENERATION OTOMATIS ======
     * Token HANYA di-generate otomatis saat:
     * 1. Form sudah disubmit (FORM_SUBMISSION = SELESAI)
     * 2. Pembayaran sudah dilakukan (PAYMENT_BRIVA = SELESAI)
     * 
     * Proses auto-generate terjadi di BrivaPaymentCheckTask
     * Tidak perlu endpoint manual request!
     * Student hanya bisa LIHAT token via GET /api/camaba/exam-token
     */

    // ===== DTO untuk messaging =====
    /**
     * ✅ NEW: Handle revision form submission
     * Student yang status validasinya REVISI dapat merevisi form pendaftaran
     * Setelah disubmit: status validasi berubah ke MENUNGGU validasi saat user mengubah form
     * PUT /api/camaba/submit-revision/{formId} - UPDATE form status to MENUNGGU after revision
     */
    @PutMapping("/submit-revision/{formId}")
    public ResponseEntity<?> submitRevision(
            @PathVariable Long formId,
            @RequestParam String fullName,
            @RequestParam String nik,
            @RequestParam String birthDate,
            @RequestParam String birthPlace,
            @RequestParam String gender,
            @RequestParam String phoneNumber,
            @RequestParam String email,
            @RequestParam(required = false) String addressMedan,
            @RequestParam(required = false) String residenceInfo,
            @RequestParam String subdistrict,
            @RequestParam String district,
            @RequestParam String city,
            @RequestParam String province,
            @RequestParam String religion,
            @RequestParam(required = false) String informationSource,
            @RequestParam(required = false) String fatherNik,
            @RequestParam String fatherName,
            @RequestParam(required = false) String fatherBirthDate,
            @RequestParam(required = false) String fatherEducation,
            @RequestParam(required = false) String fatherOccupation,
            @RequestParam(required = false) String fatherIncome,
            @RequestParam(required = false) String fatherPhone,
            @RequestParam String fatherStatus,
            @RequestParam(required = false) String motherNik,
            @RequestParam String motherName,
            @RequestParam(required = false) String motherBirthDate,
            @RequestParam(required = false) String motherEducation,
            @RequestParam(required = false) String motherOccupation,
            @RequestParam(required = false) String motherIncome,
            @RequestParam(required = false) String motherPhone,
            @RequestParam String motherStatus,
            @RequestParam String parentSubdistrict,
            @RequestParam String parentCity,
            @RequestParam String parentProvince,
            @RequestParam(required = false) String parentPhone,
            @RequestParam String schoolOrigin,
            @RequestParam String schoolMajor,
            @RequestParam String schoolYear,
            @RequestParam(required = false) String nisn,
            @RequestParam(required = false) String schoolCity,
            @RequestParam(required = false) String schoolProvince,
            @RequestParam(required = false) MultipartFile photoId,
            @RequestParam(required = false) MultipartFile certificate,
            @RequestParam(required = false) MultipartFile transcript) {
        try {
            String studentEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User user = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Student student = studentRepository.findByUser_Id(user.getId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Get admission form
            AdmissionForm form = admissionFormRepository.findById(formId)
                    .orElseThrow(() -> new RuntimeException("Admission form not found"));

            // Verify student owns this form
            if (!form.getStudent().getId().equals(student.getId())) {
                throw new RuntimeException("You don't have permission to revise this form");
            }

            log.info("🔄 Processing revision for form {}, student {}", formId, studentEmail);

            // Update form data
            form.setFullName(fullName);
            form.setNik(nik);
            form.setBirthDate(birthDate);
            form.setBirthPlace(birthPlace);
            form.setGender(gender);
            form.setPhoneNumber(phoneNumber);
            form.setEmail(email);
            form.setAddressMedan(addressMedan);
            form.setResidenceInfo(residenceInfo);
            form.setSubdistrict(subdistrict);
            form.setDistrict(district);
            form.setCity(city);
            form.setProvince(province);
            form.setReligion(religion);
            form.setInformationSource(informationSource);
            
            // Update father data
            form.setFatherNik(fatherNik);
            form.setFatherName(fatherName);
            form.setFatherBirthDate(fatherBirthDate);
            form.setFatherEducation(fatherEducation);
            form.setFatherOccupation(fatherOccupation);
            form.setFatherIncome(fatherIncome);
            form.setFatherPhone(fatherPhone);
            form.setFatherStatus(fatherStatus);
            
            // Update mother data
            form.setMotherNik(motherNik);
            form.setMotherName(motherName);
            form.setMotherBirthDate(motherBirthDate);
            form.setMotherEducation(motherEducation);
            form.setMotherOccupation(motherOccupation);
            form.setMotherIncome(motherIncome);
            form.setMotherPhone(motherPhone);
            form.setMotherStatus(motherStatus);
            
            // Update parent address data
            form.setParentSubdistrict(parentSubdistrict);
            form.setParentCity(parentCity);
            form.setParentProvince(parentProvince);
            form.setParentPhone(parentPhone);
            
            // Update school data
            form.setSchoolOrigin(schoolOrigin);
            form.setSchoolMajor(schoolMajor);
            if (schoolYear != null && !schoolYear.isEmpty()) {
                try {
                    form.setSchoolYear(Integer.parseInt(schoolYear));
                } catch (NumberFormatException e) {
                    log.warn("Invalid schoolYear format: {}", schoolYear);
                }
            }
            form.setNisn(nisn);
            form.setSchoolCity(schoolCity);
            form.setSchoolProvince(schoolProvince);
            
            // Handle file uploads (optional)
            if (photoId != null && !photoId.isEmpty()) {
                String uploadDir = "uploads/admission-forms/" + student.getId();
                new File(uploadDir).mkdirs();
                String filename = UUID.randomUUID().toString() + "_" + photoId.getOriginalFilename();
                String filePath = uploadDir + "/" + filename;
                Files.write(Paths.get(filePath), photoId.getBytes());
                form.setPhotoIdPath(filePath);
                log.info("Updated photo ID: {}", filePath);
            }
            
            if (certificate != null && !certificate.isEmpty()) {
                String uploadDir = "uploads/admission-forms/" + student.getId();
                new File(uploadDir).mkdirs();
                String filename = UUID.randomUUID().toString() + "_" + certificate.getOriginalFilename();
                String filePath = uploadDir + "/" + filename;
                Files.write(Paths.get(filePath), certificate.getBytes());
                form.setCertificatePath(filePath);
                log.info("Updated certificate: {}", filePath);
            }
            
            if (transcript != null && !transcript.isEmpty()) {
                String uploadDir = "uploads/admission-forms/" + student.getId();
                new File(uploadDir).mkdirs();
                String filename = UUID.randomUUID().toString() + "_" + transcript.getOriginalFilename();
                String filePath = uploadDir + "/" + filename;
                Files.write(Paths.get(filePath), transcript.getBytes());
                form.setTranscriptPath(filePath);
                log.info("Updated transcript: {}", filePath);
            }

            form.setUpdatedAt(LocalDateTime.now());
            final AdmissionForm savedForm = admissionFormRepository.save(form);

            // ✅ CRITICAL: Update validation status to MENUNGGU (waiting validation)
            // This allows admin to re-validate the revised form
            validationStatusTrackerService.updateStatusToMenunggu(savedForm.getId());
            
            // ✅ NEW: Update FormRepairStatus to SUDAH_PERBAIKAN when revision is submitted
            // Find FormValidation record for this admission form and mark repair as SUDAH_PERBAIKAN
            java.util.Optional<FormValidation> validationOpt = formValidationRepository.findByAdmissionFormId(savedForm.getId());
            if (validationOpt.isPresent()) {
                FormValidation validation = validationOpt.get();
                java.util.Optional<FormRepairStatus> repairStatusOpt = formRepairStatusRepository.findByFormValidationId(validation.getId());
                if (repairStatusOpt.isPresent()) {
                    FormRepairStatus repairStatus = repairStatusOpt.get();
                    repairStatus.setStatus(FormRepairStatus.RepairStatus.SUDAH_PERBAIKAN);
                    repairStatus.setUpdatedAt(LocalDateTime.now());
                    formRepairStatusRepository.save(repairStatus);
                    log.info("✅ FormRepairStatus updated to SUDAH_PERBAIKAN for validation {}", validation.getId());
                }
            }
            
            log.info("✅ Revision submitted successfully. Form {} updated, status -> MENUNGGU, repair status -> SUDAH_PERBAIKAN", formId);

            final Long formIdResult = savedForm.getId();
            final LocalDateTime updatedAtResult = savedForm.getUpdatedAt();
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Formulir revisi berhasil dikirim. Status validasi direset ke MENUNGGU VALIDASI");
                put("formId", formIdResult);
                put("updatedAt", updatedAtResult);
            }});

        } catch (Exception e) {
            final String errorMsg = e.getMessage();
            log.error("❌ Error submitting revision: {}", errorMsg, e);
            return ResponseEntity.internalServerError()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Gagal submit revisi: " + errorMsg);
                    }});
        }
    }

    /**
     * ✅ NEW: Confirm Cicilan Payment (Installment Payment Simulation)
     * POST /api/camaba/payment/cicilan-confirm
     * This endpoint marks cicilan payment as completed and unlocks daftar ulang
     */
    @PostMapping("/payment/cicilan-confirm")
    public ResponseEntity<?> confirmCicilanPayment() {
        try {
            // ✅ Get authenticated user from SecurityContext
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String email = null;
            Long userId = null;
            
            // Try to extract email/user from authentication
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else {
                email = SecurityContextHolder.getContext().getAuthentication().getName();
            }
            
            log.info("🔄 [CICILAN-CONFIRM] Processing cicilan payment for user: {}", email);
            
            // ✅ Find user by email - use final variable for lambda
            final String emailForLambda = email;
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + emailForLambda));
            
            userId = user.getId();
            log.info("✅ [CICILAN-CONFIRM] Found user: {} (ID: {})", email, userId);
            
            // ✅ Step 1: Create/Update RegistrationStatus for PAYMENT_CICILAN_1
            Optional<RegistrationStatus> existingCicilanStatus = registrationStatusRepository
                    .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_CICILAN_1);
            
            RegistrationStatus cicilanStatus;
            if (existingCicilanStatus.isPresent()) {
                cicilanStatus = existingCicilanStatus.get();
                cicilanStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                cicilanStatus.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new status if doesn't exist
                cicilanStatus = new RegistrationStatus();
                cicilanStatus.setUser(user);
                cicilanStatus.setStage(RegistrationStatus.RegistrationStage.PAYMENT_CICILAN_1);
                cicilanStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.SELESAI);
                cicilanStatus.setCreatedAt(LocalDateTime.now());
                cicilanStatus.setUpdatedAt(LocalDateTime.now());
            }
            
            registrationStatusRepository.save(cicilanStatus);
            log.info("✅ [CICILAN-CONFIRM] Cicilan payment status updated to SELESAI for user: {}", email);
            
            // ✅ Step 2: Mark DAFTAR_ULANG as unlocked (set to MENUNGGU_VERIFIKASI)
            Optional<RegistrationStatus> existingDaftarUlangStatus = registrationStatusRepository
                    .findByUserAndStage(user, RegistrationStatus.RegistrationStage.DAFTAR_ULANG);
            
            RegistrationStatus daftarUlangStatus;
            if (existingDaftarUlangStatus.isPresent()) {
                daftarUlangStatus = existingDaftarUlangStatus.get();
                // If not already completed, mark as MENUNGGU_VERIFIKASI (ready to be done)
                if (daftarUlangStatus.getStatus() != RegistrationStatus.RegistrationStatus_Enum.SELESAI) {
                    daftarUlangStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.MENUNGGU_VERIFIKASI);
                }
                daftarUlangStatus.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new DAFTAR_ULANG status as MENUNGGU_VERIFIKASI (unlocked but not started)
                daftarUlangStatus = new RegistrationStatus();
                daftarUlangStatus.setUser(user);
                daftarUlangStatus.setStage(RegistrationStatus.RegistrationStage.DAFTAR_ULANG);
                daftarUlangStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.MENUNGGU_VERIFIKASI);
                daftarUlangStatus.setCreatedAt(LocalDateTime.now());
                daftarUlangStatus.setUpdatedAt(LocalDateTime.now());
            }
            
            registrationStatusRepository.save(daftarUlangStatus);
            log.info("✅ [CICILAN-CONFIRM] Daftar Ulang item unlocked and set to MENUNGGU_VERIFIKASI for user: {}", email);
            
            // ✅ Build response with updated status info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pembayaran cicilan berhasil diproses");
            response.put("cicilanStatus", "SELESAI");
            response.put("daftarUlangUnlocked", true);
            response.put("timestamp", LocalDateTime.now());
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error confirming cicilan payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new HashMap<String, Object>() {{
                        put("success", false);
                        put("message", "Gagal memproses pembayaran cicilan: " + e.getMessage());
                    }});
        }
    }

    /**
     * Helper method to generate unique exam number
     */
    private String generateExamNumber() {
        String examNumber;
        do {
            long timestamp = System.currentTimeMillis() % 100000;
            long random = (long) (Math.random() * 100000);
            examNumber = String.format("UJI%05d%05d", timestamp, random);
        } while (examRepository.findByExamNumber(examNumber).isPresent());
        
        return examNumber;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SendMessageRequest {
        private String messageContent;
        private String messageType; // QUESTION, ANSWER, INFO
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApiResponse {
        private Boolean success;
        private String message;
    }
}


