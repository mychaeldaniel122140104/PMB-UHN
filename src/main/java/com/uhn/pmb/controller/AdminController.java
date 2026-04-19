package com.uhn.pmb.controller;

import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import com.uhn.pmb.dto.*;
import com.uhn.pmb.service.StudentRegistrationService;
import com.uhn.pmb.service.EmailService;
import com.uhn.pmb.service.RegistrationStatusService;
import com.uhn.pmb.service.JenisSeleksiService;
import com.uhn.pmb.service.ValidationStatusTrackerService;
import com.uhn.pmb.service.HasilAkhirService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final SelectionTypeRepository selectionTypeRepository;
    private final PeriodJenisSeleksiRepository periodJenisSeleksiRepository;
    private final JenisSeleksiRepository jenisSeleksiRepository;
    private final ProgramStudiRepository programStudiRepository;
    private final SelectionProgramStudiRepository selectionProgramStudiRepository;
    private final ExamLinkRepository examLinkRepository;
    private final AdmissionFormRepository admissionFormRepository;
    private final StudentRegistrationService registrationService;
    private final EmailService emailService;
    private final JenisSeleksiService jenisSeleksiService;
    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final ReEnrollmentRepository reenrollmentRepository;
    private final ReEnrollmentDocumentRepository reenrollmentDocumentRepository;
    private final FormValidationRepository formValidationRepository;
    private final FormRepairStatusRepository formRepairStatusRepository;
    private final ReEnrollmentValidationRepository reEnrollmentValidationRepository;
    private final AdminMessageRepository adminMessageRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final SystemConfigurationRepository systemConfigRepository;
    private final RegistrationStatusRepository registrationStatusRepository;
    private final CicilanRequestRepository cicilanRequestRepository;
    private final RegistrationStatusService registrationStatusService;
    private final ValidationStatusTrackerService validationStatusTrackerService;
    private final HasilAkhirService hasilAkhirService;
    private final HasilAkhirRepository hasilAkhirRepository;
    private final EntityManager entityManager;

    /**
     * Create registration period (Admin Pusat only)
     */
    @PostMapping("/periods")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createRegistrationPeriod(@Valid @RequestBody RegistrationPeriodRequest request) {
        try {
            RegistrationPeriod period = RegistrationPeriod.builder()
                    .name(request.getName())
                    .regStartDate(request.getRegStartDate())
                    .regEndDate(request.getRegEndDate())
                    .examDate(request.getExamDate())
                    .examEndDate(request.getExamEndDate())
                    .announcementDate(request.getAnnouncementDate())
                    .reenrollmentStartDate(request.getReenrollmentStartDate())
                    .reenrollmentEndDate(request.getReenrollmentEndDate())
                    .description(request.getDescription())
                    .requirements(request.getRequirements())
                    .waveType(request.getWaveType() != null ? request.getWaveType() : RegistrationPeriod.WaveType.REGULAR_TEST)
                    .status(RegistrationPeriod.Status.OPEN)
                    .build();

            registrationPeriodRepository.save(period);
            
            // ✅ AUTO-ADD: Jika tidak ada jenisSeleksiIds, auto-add KEDOKTERAN + NON_KEDOKTERAN
            List<Long> jenisSeleksiIds = request.getJenisSeleksiIds();
            if (jenisSeleksiIds == null || jenisSeleksiIds.isEmpty()) {
                // Find or create KEDOKTERAN and NON_KEDOKTERAN
                Optional<JenisSeleksi> kedokteran = jenisSeleksiRepository.findByCode("KEDOKTERAN");
                Optional<JenisSeleksi> nonKedokteran = jenisSeleksiRepository.findByCode("NON_KEDOKTERAN");
                
                if (kedokteran.isPresent() && nonKedokteran.isPresent()) {
                    jenisSeleksiIds = new ArrayList<>();
                    jenisSeleksiIds.add(kedokteran.get().getId());
                    jenisSeleksiIds.add(nonKedokteran.get().getId());
                    log.info("✅ AUTO-ADDING KEDOKTERAN + NON_KEDOKTERAN to period: {}", period.getName());
                } else {
                    log.warn("⚠️ KEDOKTERAN or NON_KEDOKTERAN not found in database. Skipping auto-add.");
                    jenisSeleksiIds = new ArrayList<>();
                }
            }
            
            // Save jenis seleksi relationships
            if (jenisSeleksiIds != null && !jenisSeleksiIds.isEmpty()) {
                for (Long jenisSeleksiId : jenisSeleksiIds) {
                    JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiId).orElse(null);
                    if (jenisSeleksi != null) {
                        PeriodJenisSeleksi pjs = PeriodJenisSeleksi.builder()
                                .period(period)
                                .jenisSeleksi(jenisSeleksi)
                                .isActive(true)
                                .build();
                        periodJenisSeleksiRepository.save(pjs);
                    }
                }
                log.info("✅ Linked {} jenis seleksi to period: {}", jenisSeleksiIds.size(), period.getName());
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Registration period created successfully"));
        } catch (Exception e) {
            log.error("Error creating registration period: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all registration periods
     */
    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllPeriods() {
        try {
            List<RegistrationPeriod> periods = registrationPeriodRepository.findAll();
            return ResponseEntity.ok(periods);
        } catch (Exception e) {
            log.error("Error fetching periods: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ==================== PERIOD JENIS SELEKSI MANAGEMENT ====================
     */

    /**
     * Get all available jenis seleksi for multi-select dropdown
     */
    @GetMapping("/api/jenis-seleksi/available")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllAvailableJenisSeleksi() {
        try {
            List<JenisSeleksi> jenisSeleksiList = jenisSeleksiRepository.findByIsActiveTrueOrderBySortOrder();
            List<Map<String, Object>> response = jenisSeleksiList.stream()
                    .map(js -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", js.getId());
                        map.put("code", js.getCode());
                        map.put("nama", js.getNama());
                        map.put("deskripsi", js.getDeskripsi());
                        map.put("logoUrl", js.getLogoUrl());
                        map.put("harga", js.getHarga());
                        map.put("isActive", js.getIsActive());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            log.info("✅ Retrieved {} available jenis seleksi", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching available jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get jenis seleksi for a specific period
     */
    @GetMapping("/api/periods/{periodId}/jenis-seleksi")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getJenisSeleksiByPeriod(@PathVariable Long periodId) {
        try {
            RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new RuntimeException("Period tidak ditemukan"));

            List<PeriodJenisSeleksi> periodJenisSeleksiList = periodJenisSeleksiRepository.findByPeriod_IdAndIsActiveTrue(periodId);
            
            List<Map<String, Object>> response = periodJenisSeleksiList.stream()
                    .map(pjs -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", pjs.getId());
                        map.put("jenisSeleksiId", pjs.getJenisSeleksi().getId());
                        map.put("code", pjs.getJenisSeleksi().getCode());
                        map.put("nama", pjs.getJenisSeleksi().getNama());
                        map.put("deskripsi", pjs.getJenisSeleksi().getDeskripsi());
                        map.put("logoUrl", pjs.getJenisSeleksi().getLogoUrl());
                        map.put("harga", pjs.getJenisSeleksi().getHarga());
                        map.put("isActive", pjs.getIsActive());
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);
            responseMap.put("periodId", periodId);
            responseMap.put("periodName", period.getName());
            responseMap.put("data", response);
            responseMap.put("total", response.size());

            log.info("✅ Retrieved {} jenis seleksi for period: {}", response.size(), period.getName());
            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            log.error("❌ Error fetching jenis seleksi for period: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Create selection type
     */
    @PostMapping("/selection-types")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createSelectionType(@Valid @RequestBody SelectionTypeRequest request) {
        try {
            RegistrationPeriod period = registrationPeriodRepository.findById(request.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period not found"));

            SelectionType selectionType = SelectionType.builder()
                    .period(period)
                    .name(request.getName())
                    .description(request.getDescription())
                    .requireRanking(request.getRequireRanking())
                    .requireTesting(request.getRequireTesting())
                    .formType(request.getFormType())
                    .price(request.getPrice())
                    .isActive(true)
                    .build();

            selectionTypeRepository.save(selectionType);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Selection type created successfully"));
        } catch (Exception e) {
            log.error("Error creating selection type: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ==================== JENIS SELEKSI ENDPOINTS ====================
     */

    /**
     * Create jenis seleksi (Admin Pusat only)
     */
    @PostMapping("/jenis-seleksi")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createJenisSeleksi(@Valid @RequestBody JenisSeleksiRequest request) {
        try {
            JenisSeleksi jenisSeleksi = JenisSeleksi.builder()
                    .code(request.getCode())
                    .nama(request.getNama())
                    .deskripsi(request.getDeskripsi())
                    .fasilitas(request.getFasilitas())
                    .logoUrl(request.getLogoUrl())
                    .harga(request.getHarga())
                    .isActive(true)
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                    .build();

            jenisSeleksi = jenisSeleksiService.create(jenisSeleksi);

            // Handle program studi M2M relationship - AUTO-LINK berdasarkan tipe
            List<Long> programStudiIds = null;
            
            // Jika ada programStudiIds di request, gunakan itu
            if (request.getProgramStudiIds() != null && !request.getProgramStudiIds().isEmpty()) {
                programStudiIds = request.getProgramStudiIds();
            } else {
                // AUTO-LINK: Jika tidak ada, hubungkan otomatis berdasarkan kode
                if (request.getCode() != null && 
                    (request.getCode().toUpperCase().contains("KEDOKTERAN") || 
                     request.getCode().toUpperCase().contains("DOKTER"))) {
                    
                    // KEDOKTERAN - link hanya ke program dengan "Dokter"
                    programStudiIds = programStudiRepository.findAll().stream()
                            .filter(p -> p.getNama() != null && p.getNama().toLowerCase().contains("dokter"))
                            .map(p -> p.getId())
                            .collect(Collectors.toList());
                    log.info("✅ Auto-linking KEDOKTERAN to medical programs: {}", programStudiIds);
                    
                } else {
                    // NON-KEDOKTERAN - link ke semua program KECUALI yang "Dokter"
                    programStudiIds = programStudiRepository.findAll().stream()
                            .filter(p -> p.getNama() == null || !p.getNama().toLowerCase().contains("dokter"))
                            .map(p -> p.getId())
                            .collect(Collectors.toList());
                    log.info("✅ Auto-linking NON-KEDOKTERAN to non-medical programs: {}", programStudiIds);
                }
            }

            // Save relationships
            if (programStudiIds != null && !programStudiIds.isEmpty()) {
                for (Long programStudiId : programStudiIds) {
                    ProgramStudi programStudi = programStudiRepository.findById(programStudiId).orElse(null);
                    if (programStudi != null) {
                        SelectionProgramStudi sps = SelectionProgramStudi.builder()
                                .jenisSeleksi(jenisSeleksi)
                                .programStudi(programStudi)
                                .isActive(true)
                                .build();
                        selectionProgramStudiRepository.save(sps);
                    }
                }
                log.info("✅ Linked {} program studi to jenis seleksi: {}", programStudiIds.size(), jenisSeleksi.getNama());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Jenis Seleksi created successfully"));
        } catch (Exception e) {
            log.error("Error creating jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all jenis seleksi
     */
    @GetMapping("/jenis-seleksi")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllJenisSeleksi() {
        try {
            List<JenisSeleksi> jenisSeleksiList = jenisSeleksiService.getAll();
            return ResponseEntity.ok(jenisSeleksiList);
        } catch (Exception e) {
            log.error("Error fetching jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get active jenis seleksi only
     */
    @GetMapping("/jenis-seleksi/active")
    public ResponseEntity<?> getActiveJenisSeleksi() {
        try {
            List<JenisSeleksi> jenisSeleksiList = jenisSeleksiService.getAllActive();
            return ResponseEntity.ok(jenisSeleksiList);
        } catch (Exception e) {
            log.error("Error fetching active jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get jenis seleksi by id
     */
    @GetMapping("/jenis-seleksi/{id}")
    public ResponseEntity<?> getJenisSeleksiById(@PathVariable Long id) {
        try {
            JenisSeleksi jenisSeleksi = jenisSeleksiService.getById(id)
                    .orElseThrow(() -> new RuntimeException("Jenis Seleksi not found"));
            return ResponseEntity.ok(jenisSeleksi);
        } catch (Exception e) {
            log.error("Error fetching jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Update jenis seleksi
     */
    @PutMapping("/jenis-seleksi/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    @Transactional
    public ResponseEntity<?> updateJenisSeleksi(@PathVariable Long id, 
                                                 @Valid @RequestBody JenisSeleksiRequest request) {
        try {
            // 🔍 DEBUG: Log method entry
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            
            log.info("═══════════════════════════════════════════════════════════");
            log.info("✅ [PUT-JENIS-SELEKSI] METHOD ENTRY - CAN REACH HERE = PREAUTHORIZE PASSED!");
            log.info("   Email: {}", email);
            log.info("   Authorities: {}", authorities.stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toList()));
            log.info("   Jenis Seleksi ID: {}", id);
            log.info("═══════════════════════════════════════════════════════════");
            
            JenisSeleksi updates = JenisSeleksi.builder()
                    .code(request.getCode())
                    .nama(request.getNama())
                    .deskripsi(request.getDeskripsi())
                    .fasilitas(request.getFasilitas())
                    .logoUrl(request.getLogoUrl())
                    .harga(request.getHarga())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                    .build();

            JenisSeleksi jenisSeleksi = jenisSeleksiService.update(id, updates);

            // Handle program studi M2M relationship update
            // Delete old relationships
            selectionProgramStudiRepository.deleteByJenisSeleksi_Id(id);

            // Create new relationships
            if (request.getProgramStudiIds() != null && !request.getProgramStudiIds().isEmpty()) {
                for (Long programStudiId : request.getProgramStudiIds()) {
                    ProgramStudi programStudi = programStudiRepository.findById(programStudiId).orElse(null);
                    if (programStudi != null) {
                        SelectionProgramStudi sps = SelectionProgramStudi.builder()
                                .jenisSeleksi(jenisSeleksi)
                                .programStudi(programStudi)
                                .isActive(true)
                                .build();
                        selectionProgramStudiRepository.save(sps);
                    }
                }
                log.info("✅ Updated {} program studi for jenis seleksi: {}", request.getProgramStudiIds().size(), jenisSeleksi.getNama());
            }

            return ResponseEntity.ok(new ApiResponse(true, "Jenis Seleksi updated successfully"));
        } catch (Exception e) {
            log.error("Error updating jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Delete jenis seleksi
     */
    @DeleteMapping("/jenis-seleksi/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    @Transactional
    public ResponseEntity<?> deleteJenisSeleksi(@PathVariable Long id) {
        try {
            jenisSeleksiService.delete(id);
            return ResponseEntity.ok(new ApiResponse(true, "Jenis Seleksi deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ==================== PROGRAM STUDI ENDPOINTS ====================
     */

    /**
     * Create program studi (Admin Pusat only)
     */
    @PostMapping("/program-studi")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createProgramStudi(@Valid @RequestBody ProgramStudiRequest request) {
        try {
            if (programStudiRepository.existsByKode(request.getKode())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Kode program studi sudah digunakan"));
            }

            ProgramStudi programStudi = ProgramStudi.builder()
                    .kode(request.getKode())
                    .nama(request.getNama())
                    .deskripsi(request.getDeskripsi())
                    .isMedical(request.getIsMedical() != null ? request.getIsMedical() : false)
                    .isActive(true)
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                    .hargaTotalPerTahun(request.getHargaTotalPerTahun() != null ? request.getHargaTotalPerTahun() : 0L)
                    .cicilan1(request.getCicilan1() != null ? request.getCicilan1() : 0L)
                    .cicilan2(request.getCicilan2() != null ? request.getCicilan2() : 0L)
                    .cicilan3(request.getCicilan3() != null ? request.getCicilan3() : 0L)
                    .cicilan4(request.getCicilan4() != null ? request.getCicilan4() : 0L)
                    .cicilan5(request.getCicilan5() != null ? request.getCicilan5() : 0L)
                    .cicilan6(request.getCicilan6() != null ? request.getCicilan6() : 0L)
                    .build();

            programStudiRepository.save(programStudi);
            log.info("✅ Program Studi created: {}", programStudi.getNama());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Program Studi created successfully"));
        } catch (Exception e) {
            log.error("❌ Error creating program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all program studi
     */
    @GetMapping("/program-studi")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllProgramStudi() {
        try {
            List<ProgramStudi> programStudiList = programStudiRepository.findAllByOrderBySortOrder();
            List<Map<String, Object>> response = programStudiList.stream()
                    .map(ps -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ps.getId());
                        map.put("kode", ps.getKode());
                        map.put("nama", ps.getNama());
                        map.put("deskripsi", ps.getDeskripsi() != null ? ps.getDeskripsi() : "");
                        map.put("isMedical", ps.getIsMedical());
                        map.put("isActive", ps.getIsActive());
                        map.put("sortOrder", ps.getSortOrder());
                        map.put("hargaTotalPerTahun", ps.getHargaTotalPerTahun() != null ? ps.getHargaTotalPerTahun() : 0L);
                        map.put("cicilan1", ps.getCicilan1() != null ? ps.getCicilan1() : 0L);
                        map.put("cicilan2", ps.getCicilan2() != null ? ps.getCicilan2() : 0L);
                        map.put("cicilan3", ps.getCicilan3() != null ? ps.getCicilan3() : 0L);
                        map.put("cicilan4", ps.getCicilan4() != null ? ps.getCicilan4() : 0L);
                        map.put("cicilan5", ps.getCicilan5() != null ? ps.getCicilan5() : 0L);
                        map.put("cicilan6", ps.getCicilan6() != null ? ps.getCicilan6() : 0L);
                        return map;
                    })
                    .collect(Collectors.toList());
            log.info("✅ Retrieved {} program studi", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get active program studi only
     */
    @GetMapping("/program-studi/active")
    public ResponseEntity<?> getActiveProgramStudi() {
        try {
            List<ProgramStudi> programStudiList = programStudiRepository.findByIsActiveTrueOrderBySortOrder();
            List<Map<String, Object>> response = programStudiList.stream()
                    .map(ps -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ps.getId());
                        map.put("kode", ps.getKode());
                        map.put("nama", ps.getNama());
                        map.put("deskripsi", ps.getDeskripsi() != null ? ps.getDeskripsi() : "");
                        map.put("isMedical", ps.getIsMedical());
                        return map;
                    })
                    .collect(Collectors.toList());
            log.info("✅ Retrieved {} active program studi", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching active program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get program studi by id
     */
    @GetMapping("/program-studi/{id}")
    public ResponseEntity<?> getProgramStudiById(@PathVariable Long id) {
        try {
            ProgramStudi programStudi = programStudiRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Program Studi not found"));
            return ResponseEntity.ok(programStudi);
        } catch (Exception e) {
            log.error("❌ Error fetching program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Update program studi
     */
    @PutMapping("/program-studi/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateProgramStudi(@PathVariable Long id,
                                               @Valid @RequestBody ProgramStudiRequest request) {
        try {
            ProgramStudi programStudi = programStudiRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Program Studi not found"));

            // Check if kode is being changed and new kode already exists
            if (!programStudi.getKode().equals(request.getKode()) &&
                    programStudiRepository.existsByKode(request.getKode())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Kode program studi sudah digunakan"));
            }

            programStudi.setKode(request.getKode());
            programStudi.setNama(request.getNama());
            programStudi.setDeskripsi(request.getDeskripsi());
            programStudi.setIsMedical(request.getIsMedical() != null ? request.getIsMedical() : false);
            programStudi.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            programStudi.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
            programStudi.setHargaTotalPerTahun(request.getHargaTotalPerTahun() != null ? request.getHargaTotalPerTahun() : 0L);
            programStudi.setCicilan1(request.getCicilan1() != null ? request.getCicilan1() : 0L);
            programStudi.setCicilan2(request.getCicilan2() != null ? request.getCicilan2() : 0L);
            programStudi.setCicilan3(request.getCicilan3() != null ? request.getCicilan3() : 0L);
            programStudi.setCicilan4(request.getCicilan4() != null ? request.getCicilan4() : 0L);
            programStudi.setCicilan5(request.getCicilan5() != null ? request.getCicilan5() : 0L);
            programStudi.setCicilan6(request.getCicilan6() != null ? request.getCicilan6() : 0L);
            programStudi.setUpdatedAt(LocalDateTime.now());

            programStudiRepository.save(programStudi);
            log.info("✅ Program Studi updated: {}", programStudi.getNama());
            return ResponseEntity.ok(new ApiResponse(true, "Program Studi updated successfully"));
        } catch (Exception e) {
            log.error("❌ Error updating program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Delete program studi
     */
    @DeleteMapping("/program-studi/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteProgramStudi(@PathVariable Long id) {
        try {
            ProgramStudi programStudi = programStudiRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Program Studi not found"));

            // Check if program studi is used in any jenis seleksi
            if (selectionProgramStudiRepository.existsByProgramStudi_Id(id)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Program Studi masih digunakan di jenis seleksi lain"));
            }

            programStudiRepository.delete(programStudi);
            log.info("✅ Program Studi deleted: {}", programStudi.getNama());
            return ResponseEntity.ok(new ApiResponse(true, "Program Studi deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Error deleting program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ==================== JENIS SELEKSI - PROGRAM STUDI RELATIONSHIP ENDPOINTS ====================
     */

    /**
     * Get available program studi (all active)
     */
    @GetMapping("/api/program-studi/available")
    public ResponseEntity<?> getAvailableProgramStudi() {
        try {
            List<ProgramStudi> programStudiList = programStudiRepository.findByIsActiveTrueOrderBySortOrder();
            List<Map<String, Object>> response = programStudiList.stream()
                    .map(ps -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", ps.getId());
                        map.put("kode", ps.getKode());
                        map.put("nama", ps.getNama());
                        map.put("isMedical", ps.getIsMedical());
                        return map;
                    })
                    .collect(Collectors.toList());
            log.info("✅ Retrieved {} available program studi", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching available program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get program studi for a specific jenis seleksi
     */
    @GetMapping("/api/jenis-seleksi/{jenisSeleksiId}/program-studi")
    public ResponseEntity<?> getProgramStudiByJenisSeleksi(@PathVariable Long jenisSeleksiId) {
        try {
            JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiId)
                    .orElseThrow(() -> new RuntimeException("Jenis Seleksi tidak ditemukan"));

            List<SelectionProgramStudi> selectionProgramStudiList = 
                    selectionProgramStudiRepository.findByJenisSeleksi_IdAndIsActiveTrue(jenisSeleksiId);

            List<Map<String, Object>> response = selectionProgramStudiList.stream()
                    .map(sps -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", sps.getId());
                        map.put("programStudiId", sps.getProgramStudi().getId());
                        map.put("kode", sps.getProgramStudi().getKode());
                        map.put("nama", sps.getProgramStudi().getNama());
                        map.put("isMedical", sps.getProgramStudi().getIsMedical());
                        map.put("isActive", sps.getIsActive());
                        return map;
                    })
                    .collect(Collectors.toList());

            log.info("✅ Retrieved {} program studi for jenis seleksi {}", response.size(), jenisSeleksi.getNama());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching program studi for jenis seleksi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get admission forms for validation (Admin Validasi)
     */
    @GetMapping("/forms-to-validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFormsToValidate() {
        try {
            List<AdmissionForm> forms = admissionFormRepository
                    .findByStatus(AdmissionForm.FormStatus.SUBMITTED);
            return ResponseEntity.ok(forms);
        } catch (Exception e) {
            log.error("Error fetching forms: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Validate admission form
     */
    @PutMapping("/forms/{formId}/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validateForm(@PathVariable Long formId, @RequestBody ValidationRequest request) {
        try {
            AdmissionForm form = admissionFormRepository.findById(formId)
                    .orElseThrow(() -> new RuntimeException("Form not found"));

            if (request.getApproved()) {
                form.setStatus(AdmissionForm.FormStatus.VERIFIED);
                emailService.sendSimpleEmail(
                        form.getStudent().getUser().getEmail(),
                        "Validasi Data Berhasil",
                        "Data pendaftaran Anda telah divalidasi dan disetujui."
                );
            } else {
                form.setStatus(AdmissionForm.FormStatus.REJECTED);
                emailService.sendSimpleEmail(
                        form.getStudent().getUser().getEmail(),
                        "Validasi Data Ditolak",
                        "Data pendaftaran Anda ditolak. Alasan: " + request.getReason()
                );
            }

            admissionFormRepository.save(form);
            return ResponseEntity.ok(new ApiResponse(true, "Form validation completed"));
        } catch (Exception e) {
            log.error("Error validating form: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Publish exam results
     */
    @PostMapping("/publish-results/{periodId}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> publishResults(@PathVariable Long periodId) {
        try {
            RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new RuntimeException("Period not found"));

            registrationService.publishExamResults(period);
            return ResponseEntity.ok(new ApiResponse(true, "Results published successfully"));
        } catch (Exception e) {
            log.error("Error publishing results: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get re-enrollment data
     */
    @GetMapping("/reenrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReenrollments() {
        try {
            List<ReEnrollment> reenrollments = reenrollmentRepository
                    .findByStatus(ReEnrollment.ReEnrollmentStatus.SUBMITTED);
            return ResponseEntity.ok(reenrollments);
        } catch (Exception e) {
            log.error("Error fetching reenrollments: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Validate re-enrollment
     */
    @PutMapping("/reenrollments/{reenrollmentId}/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validateReEnrollment(@PathVariable Long reenrollmentId, 
                                                  @RequestBody ValidationRequest request) {
        try {
            ReEnrollment reenrollment = reenrollmentRepository.findById(reenrollmentId)
                    .orElseThrow(() -> new RuntimeException("Re-enrollment not found"));

            if (request.getApproved()) {
                reenrollment.setStatus(ReEnrollment.ReEnrollmentStatus.VALIDATED);
                reenrollment.setValidatedAt(LocalDateTime.now());
            } else {
                reenrollment.setStatus(ReEnrollment.ReEnrollmentStatus.REJECTED);
                reenrollment.setValidationNotes(request.getReason());
            }

            reenrollmentRepository.save(reenrollment);
            return ResponseEntity.ok(new ApiResponse(true, "Re-enrollment validation completed"));
        } catch (Exception e) {
            log.error("Error validating re-enrollment: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get in-progress re-enrollments (for admin dashboard)
     * GET /admin/api/reenrollments/in-progress
     */
    @GetMapping("/api/reenrollments/in-progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> getInProgressReenrollments() {
        try {
            log.info("🔥 ==========================================");
            log.info("🔥 CONTROLLER /admin/api/reenrollments/in-progress CALLED");
            log.info("🔥 ==========================================");
            
            List<ReEnrollment> inProgressReenrollments = reenrollmentRepository
                    .findByStatus(ReEnrollment.ReEnrollmentStatus.SUBMITTED);
            
            log.info("✅ Found {} in-progress re-enrollments", inProgressReenrollments.size());
            return ResponseEntity.ok(inProgressReenrollments);
        } catch (Exception e) {
            log.error("Error fetching in-progress re-enrollments: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get validated/passed students for exam (for admin dashboard)
     * GET /admin/api/exam/student-list
     */
    @GetMapping("/api/exam/student-list")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> getStudentListForExam() {
        try {
            log.info("🔥 ==========================================");
            log.info("🔥 CONTROLLER /admin/api/exam/student-list CALLED");
            log.info("🔥 ==========================================");
            
            // Get students with passed exam results
            List<ExamResult> passedResults = examResultRepository.findAll().stream()
                    .filter(result -> result.getStatus() != null && result.getStatus() == ExamResult.ResultStatus.PASSED)
                    .toList();
            
            log.info("✅ Found {} students who passed exam", passedResults.size());
            return ResponseEntity.ok(passedResults);
        } catch (Exception e) {
            log.error("Error fetching student list for exam: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== VALIDASI DATA FORMULIR & PEMBAYARAN ==========

    /**
     * Get forms untuk validasi (dengan status pembayaran)
     */
    @GetMapping("/api/validasi/formulir")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> getFormsForValidation() {
        try {
            log.info("🔥 ==========================================");
            log.info("🔥 CONTROLLER /admin/api/validasi/formulir CALLED");
            log.info("🔥 ==========================================");
            // 🔍 Get ALL forms and filter by validation criteria:
            // - Payment COMPLETED (udah bayar)
            // - Form COMPLETED (udah isi form)
            List<FormValidation> validations = new ArrayList<>();
            validations.addAll(formValidationRepository.findAll());
            
            // Sort by created date (descending)
            validations.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            
            log.info("📊 Admin validation API called - Found {} total form records", validations.size());
            
            List<Map<String, Object>> response = validations.stream()
                .filter(fv -> {
                    // ✅ FILTER LOGIC: Only show forms that meet validation criteria
                    AdmissionForm form = fv.getAdmissionForm();
                    Student student = fv.getStudent();
                    
                    if (student == null || student.getUser() == null || form == null) {
                        return false; // Skip if missing critical data
                    }
                    
                    User user = student.getUser();
                    
                    // Check FORM_SUBMISSION status (form filled)
                    java.util.Optional<RegistrationStatus> formRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.FORM_SUBMISSION);
                    boolean formCompleted = formRegStatus.isPresent() && formRegStatus.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI;
                    
                    // Check PAYMENT_BRIVA status (payment done)
                    java.util.Optional<RegistrationStatus> paymentRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_BRIVA);
                    boolean paymentCompleted = paymentRegStatus.isPresent() && paymentRegStatus.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI;
                    
                    // ✅ FIXED: Show if FORM is completed (regardless of payment status)
                    // Frontend will handle payment filtering for payment reminder table
                    boolean shouldShow = formCompleted;
                    
                    log.debug("   Filter check - Student: {}, Payment: {}, Form: {}, Show: {}", 
                        student.getFullName(), paymentCompleted, formCompleted, shouldShow);
                    
                    return shouldShow;
                })
                .map(fv -> {
                Map<String, Object> data = new HashMap<>();
                AdmissionForm form = fv.getAdmissionForm();
                Student student = fv.getStudent();
                
                data.put("id", fv.getId());
                data.put("formValidationId", fv.getId());
                data.put("formId", form != null ? form.getId() : null);
                data.put("admissionFormId", form != null ? form.getId() : null); // ✅ NEW: Add admissionFormId
                data.put("studentId", student != null ? student.getId() : null);
                
                // 👤 Student info
                String studentName = student != null ? student.getFullName() : "-";
                String studentEmail = student != null && student.getUser() != null ? student.getUser().getEmail() : "-";
                data.put("studentName", studentName);
                data.put("studentEmail", studentEmail);
                data.put("studentPhone", student != null ? student.getPhoneNumber() : "-");
                
                // 📋 Form info - Basic
                data.put("formFullName", form != null ? form.getFullName() : "-");
                data.put("programStudi1", form != null ? form.getProgramStudi1() : "-");
                data.put("programStudi2", form != null ? form.getProgramStudi2() : "-");
                
                // 💳 Payment & Financial info
                String vaNumber = fv.getVirtualAccountNumber() != null ? fv.getVirtualAccountNumber() : "-";
                data.put("virtualAccountNumber", vaNumber);
                
                // ✅ FIX: Get BRIVA from CICILAN_REQUEST table (not from VA)
                String brivaNumber = "-";
                if (student != null) {
                    try {
                        List<CicilanRequest> cicilanList = cicilanRequestRepository
                            .findByStudentIdAndStatus(student.getId(), CicilanRequest.CicilanRequestStatus.APPROVED);
                        if (cicilanList != null && !cicilanList.isEmpty()) {
                            CicilanRequest cicilan = cicilanList.get(0); // Get first APPROVED cicilan
                            brivaNumber = cicilan.getBriva() != null ? cicilan.getBriva() : "-";
                            log.debug("✅ [BRIVA] Found from CICILAN_REQUEST: {}", brivaNumber);
                        } else {
                            log.debug("⚠️ [BRIVA] No APPROVED cicilan found for student: {}", student.getId());
                        }
                    } catch (Exception e) {
                        log.debug("⚠️ [BRIVA] Error fetching from CICILAN_REQUEST: {}", e.getMessage());
                    }
                }
                data.put("brivaNumber", brivaNumber); // ✅ FIXED: Now from CICILAN_REQUEST
                
                // Get payment amount from FormValidation or fallback to VirtualAccount
                Long paymentAmountValue = fv.getPaymentAmount();
                if (paymentAmountValue == null || paymentAmountValue == 0) {
                    // Try to get from VirtualAccount
                    if (!vaNumber.equals("-")) {
                        try {
                            VirtualAccount va = virtualAccountRepository.findByVaNumber(vaNumber)
                                    .orElse(null);
                            if (va != null && va.getAmount() != null) {
                                paymentAmountValue = va.getAmount().longValue();
                            }
                        } catch (Exception e) {
                            log.debug("Could not fetch VA amount for {}: {}", vaNumber, e.getMessage());
                        }
                    }
                }
                
                data.put("paymentAmount", paymentAmountValue != null ? String.format("Rp %,d", paymentAmountValue) : "-");
                data.put("paymentAmountRaw", paymentAmountValue);
                
                // ✅ STATUS INFO - from FORM_VALIDATION table (actual admin approval status) + REGISTRATION_STATUS
                // validationStatus = Admin's approval decision (PENDING, APPROVED, REJECTION_NEEDED, REJECTED)
                // paymentStatus = Student's payment status (PENDING, PAID, VERIFIED)
                // formStatus (temp) = Form submission completion status (PENDING, COMPLETED)
                
                String validationStatusStr = fv.getValidationStatus() != null ? fv.getValidationStatus().toString() : "PENDING";
                String paymentStatus = "PENDING";
                String installmentStatus = "PENDING"; // ✅ NEW: Cicilan status
                String examStatus = "PENDING";
                String formStatus = "PENDING"; // Form submission status (not returned, just for exam status check)
                
                if (student != null && student.getUser() != null) {
                    User user = student.getUser();
                    
                    // Check FORM_SUBMISSION status
                    java.util.Optional<RegistrationStatus> formRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.FORM_SUBMISSION);
                    if (formRegStatus.isPresent() && formRegStatus.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI) {
                        formStatus = "COMPLETED";
                    }
                    
                    // Check PAYMENT_BRIVA status  
                    java.util.Optional<RegistrationStatus> paymentRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_BRIVA);
                    if (paymentRegStatus.isPresent() && paymentRegStatus.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI) {
                        paymentStatus = "PAID";
                    }
                    
                    // ✅ NEW: Check PAYMENT_CICILAN_1 status
                    java.util.Optional<RegistrationStatus> cicilanRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PAYMENT_CICILAN_1);
                    if (cicilanRegStatus.isPresent()) {
                        RegistrationStatus.RegistrationStatus_Enum cicilanRegisterStatus = cicilanRegStatus.get().getStatus();
                        installmentStatus = cicilanRegisterStatus == RegistrationStatus.RegistrationStatus_Enum.SELESAI ? "PAID" : "PENDING";
                    }
                    
                    // ✅ Check PSYCHO_EXAM status (actual exam completion from RegistrationStatus)
                    java.util.Optional<RegistrationStatus> examRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.PSYCHO_EXAM);
                    if (examRegStatus.isPresent() && examRegStatus.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI) {
                        examStatus = "COMPLETED";
                    } else {
                        examStatus = "PENDING";
                    }
                }
                
                // ✅ CRITICAL FIX: Return actual validationStatus from FormValidation entity
                data.put("validationStatus", validationStatusStr);
                data.put("paymentStatus", paymentStatus);
                data.put("installmentStatus", installmentStatus); // ✅ NEW: Add cicilan status
                data.put("paymentMethod", "BRIVA"); // ✅ NEW: Add payment method (default to BRIVA)
                
                // ✅ NEW: Add repair status (track if student has fixed form after revision)
                String repairStatusStr = "BELUM_PERBAIKAN"; // Default
                java.util.Optional<FormRepairStatus> repairStatusOpt = formRepairStatusRepository.findByFormValidationId(fv.getId());
                if (repairStatusOpt.isPresent()) {
                    repairStatusStr = repairStatusOpt.get().getStatus().toString();
                }
                data.put("repairStatus", repairStatusStr);
                
                data.put("revisionReason", fv.getRejectionReason() != null ? fv.getRejectionReason() : "");
                data.put("rejectionReason", fv.getRejectionReason() != null ? fv.getRejectionReason() : "");
                
                // ✅ RE-ENROLLMENT STATUS: Check if student has started re-enrollment
                String reEnrollmentStatus = "NOT_STARTED"; // Default
                if (student != null && student.getUser() != null) {
                    User user = student.getUser();
                    // Check DAFTAR_ULANG stage
                    java.util.Optional<RegistrationStatus> reEnrollRegStatus = registrationStatusRepository
                            .findByUserAndStage(user, RegistrationStatus.RegistrationStage.DAFTAR_ULANG);
                    if (reEnrollRegStatus.isPresent() && reEnrollRegStatus.get().getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI) {
                        reEnrollmentStatus = "COMPLETED";
                    } else if (reEnrollRegStatus.isPresent()) {
                        reEnrollmentStatus = "IN_PROGRESS";
                    }
                }
                data.put("reEnrollmentStatus", reEnrollmentStatus);
                
                // 🎫 Token info
                data.put("examToken", fv.getExamToken() != null ? fv.getExamToken() : "");
                data.put("hasToken", fv.getExamToken() != null && !fv.getExamToken().isEmpty());
                
                // ✅ Exam Status - determined by token existence
                data.put("examStatus", examStatus);
                
                // 📚 Wave/Jalur info - ✅ FIXED: Get actual waveType from RegistrationPeriod
                String waveType = "REGULAR_TEST"; // Default
                if (form != null && form.getPeriod() != null && form.getPeriod().getWaveType() != null) {
                    waveType = form.getPeriod().getWaveType().toString();
                }
                data.put("waveType", waveType);
                
                // ⏰ Timestamps
                data.put("createdAt", fv.getCreatedAt());
                data.put("submittedAt", form != null ? form.getSubmittedAt() : null);
                data.put("updatedAt", fv.getUpdatedAt());
                
                log.debug("  ✓ Student: {} ({}), Program: {}, Amount: Rp {}, WaveType: {}, Token: {}", 
                    studentName, studentEmail, (form != null ? form.getProgramStudi1() : "-"), fv.getPaymentAmount(),
                    waveType, fv.getExamToken() != null ? "✅" : "❌");
                
                return data;
            }).collect(Collectors.toList());
            
            log.info("✅ Returning {} forms (filtered) to admin validation dashboard", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching forms for validation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get complete form details for modal view
     */
    @GetMapping("/api/validasi/formulir/{formValidationId}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> getFormDetails(@PathVariable Long formValidationId) {
        try {
            FormValidation validation = formValidationRepository.findById(formValidationId)
                    .orElseThrow(() -> new RuntimeException("Form validation tidak ditemukan"));
            
            AdmissionForm form = validation.getAdmissionForm();
            Student student = validation.getStudent();
            
            Map<String, Object> details = new HashMap<>();
            
            // ===== HEADER INFO =====
            details.put("id", validation.getId());
            details.put("studentId", student.getId());
            details.put("formId", form.getId());
            details.put("selectionTypeId", form.getSelectionTypeId() != null ? form.getSelectionTypeId() : "-");
            details.put("jenisSeleksiId", form.getJenisSeleksiId() != null ? form.getJenisSeleksiId() : "-");
            details.put("periodId", form.getPeriod() != null ? form.getPeriod().getId() : "-");
            
            // ===== STUDENT PERSONAL INFO =====
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("fullName", student.getFullName());
            studentInfo.put("email", student.getUser().getEmail());
            studentInfo.put("phoneNumber", student.getPhoneNumber());
            studentInfo.put("nik", form.getNik());
            studentInfo.put("birthDate", form.getBirthDate() != null ? form.getBirthDate() : "-");
            studentInfo.put("birthPlace", form.getBirthPlace() != null ? form.getBirthPlace() : "-");
            studentInfo.put("gender", form.getGender() != null ? form.getGender() : "-");
            studentInfo.put("religion", form.getReligion() != null ? form.getReligion() : "-");
            studentInfo.put("informationSource", form.getInformationSource() != null ? form.getInformationSource() : "-");
            details.put("student", studentInfo);
            
            // ===== FORM DATA - PILIHAN PROGRAM STUDI =====
            Map<String, Object> programInfo = new HashMap<>();
            programInfo.put("pilihan1", form.getProgramStudi1() != null ? form.getProgramStudi1() : "-");
            programInfo.put("pilihan2", form.getProgramStudi2() != null ? form.getProgramStudi2() : "-");
            programInfo.put("pilihan3", form.getProgramStudi3() != null ? form.getProgramStudi3() : "-");
            programInfo.put("tipeForm", form.getFormType() != null ? form.getFormType().toString() : "-");
            details.put("programChoice", programInfo);
            
            // ===== ALAMAT INFO (TINGGAL) =====
            Map<String, Object> addressInfo = new HashMap<>();
            addressInfo.put("alamatMedan", form.getAddressMedan() != null ? form.getAddressMedan() : "-");
            addressInfo.put("residenceInfo", form.getResidenceInfo() != null ? form.getResidenceInfo() : "-");
            addressInfo.put("kelurahan", form.getSubdistrict() != null ? form.getSubdistrict() : "-");
            addressInfo.put("kecamatan", form.getDistrict() != null ? form.getDistrict() : "-");
            addressInfo.put("kota", form.getCity() != null ? form.getCity() : "-");
            addressInfo.put("provinsi", form.getProvince() != null ? form.getProvince() : "-");
            details.put("address", addressInfo);
            
            // ===== INFORMASI ORANG TUA =====
            Map<String, Object> parentInfo = new HashMap<>();
            // FATHER
            parentInfo.put("fatherName", form.getFatherName() != null ? form.getFatherName() : "-");
            parentInfo.put("fatherNik", form.getFatherNik() != null ? form.getFatherNik() : "-");
            parentInfo.put("fatherBirthDate", form.getFatherBirthDate() != null ? form.getFatherBirthDate() : "-");
            parentInfo.put("fatherEducation", form.getFatherEducation() != null ? form.getFatherEducation() : "-");
            parentInfo.put("fatherOccupation", form.getFatherOccupation() != null ? form.getFatherOccupation() : "-");
            parentInfo.put("fatherIncome", form.getFatherIncome() != null ? form.getFatherIncome() : "-");
            parentInfo.put("fatherPhone", form.getFatherPhone() != null ? form.getFatherPhone() : "-");
            parentInfo.put("fatherStatus", form.getFatherStatus() != null ? form.getFatherStatus() : "-");
            // MOTHER
            parentInfo.put("motherName", form.getMotherName() != null ? form.getMotherName() : "-");
            parentInfo.put("motherNik", form.getMotherNik() != null ? form.getMotherNik() : "-");
            parentInfo.put("motherBirthDate", form.getMotherBirthDate() != null ? form.getMotherBirthDate() : "-");
            parentInfo.put("motherEducation", form.getMotherEducation() != null ? form.getMotherEducation() : "-");
            parentInfo.put("motherOccupation", form.getMotherOccupation() != null ? form.getMotherOccupation() : "-");
            parentInfo.put("motherIncome", form.getMotherIncome() != null ? form.getMotherIncome() : "-");
            parentInfo.put("motherPhone", form.getMotherPhone() != null ? form.getMotherPhone() : "-");
            parentInfo.put("motherStatus", form.getMotherStatus() != null ? form.getMotherStatus() : "-");
            // PARENT ADDRESS
            parentInfo.put("parentSubdistrict", form.getParentSubdistrict() != null ? form.getParentSubdistrict() : "-");
            parentInfo.put("parentCity", form.getParentCity() != null ? form.getParentCity() : "-");
            parentInfo.put("parentProvince", form.getParentProvince() != null ? form.getParentProvince() : "-");
            parentInfo.put("parentPhone", form.getParentPhone() != null ? form.getParentPhone() : "-");
            parentInfo.put("parentAddress", (form.getParentSubdistrict() != null ? form.getParentSubdistrict() : "") + ", " + 
                    (form.getParentCity() != null ? form.getParentCity() : "") + ", " + 
                    (form.getParentProvince() != null ? form.getParentProvince() : ""));
            details.put("parents", parentInfo);
            
            // ===== DATA ASAL SEKOLAH =====
            Map<String, Object> schoolInfo = new HashMap<>();
            schoolInfo.put("namaSekolah", form.getSchoolOrigin() != null ? form.getSchoolOrigin() : "-");
            schoolInfo.put("jurusan", form.getSchoolMajor() != null ? form.getSchoolMajor() : "-");
            schoolInfo.put("tahunLulus", form.getSchoolYear() != null ? form.getSchoolYear() : "-");
            schoolInfo.put("nisn", form.getNisn() != null ? form.getNisn() : "-");
            schoolInfo.put("kota", form.getSchoolCity() != null ? form.getSchoolCity() : "-");
            schoolInfo.put("provinsi", form.getSchoolProvince() != null ? form.getSchoolProvince() : "-");
            details.put("school", schoolInfo);
            
            // ===== DOKUMEN & FILE =====
            Map<String, Object> documentInfo = new HashMap<>();
            documentInfo.put("photoIdPath", form.getPhotoIdPath() != null ? form.getPhotoIdPath() : "-");
            documentInfo.put("certificatePath", form.getCertificatePath() != null ? form.getCertificatePath() : "-");
            documentInfo.put("transcriptPath", form.getTranscriptPath() != null ? form.getTranscriptPath() : "-");
            documentInfo.put("nilaiFilePath", form.getNilaiFilePath() != null ? form.getNilaiFilePath() : "-");
            documentInfo.put("rankingFilePath", form.getRankingFilePath() != null ? form.getRankingFilePath() : "-");
            details.put("documents", documentInfo);
            
            // ===== INFORMASI TAMBAHAN =====
            details.put("additionalInfo", form.getAdditionalInfo() != null ? form.getAdditionalInfo() : "-");
            
            // ===== STATUS & TIMESTAMPS =====
            details.put("formStatus", form.getStatus() != null ? form.getStatus().toString() : "-");
            details.put("submittedAt", form.getSubmittedAt() != null ? form.getSubmittedAt() : "-");
            details.put("createdAt", form.getCreatedAt() != null ? form.getCreatedAt() : "-");
            details.put("updatedAt", form.getUpdatedAt() != null ? form.getUpdatedAt() : "-");
            
            // ===== PAYMENT & VALIDATION INFO =====
            Map<String, Object> validationInfo = new HashMap<>();
            validationInfo.put("validationStatus", validation.getValidationStatus() != null ? validation.getValidationStatus().toString() : "-");
            validationInfo.put("paymentStatus", validation.getPaymentStatus() != null ? validation.getPaymentStatus().toString() : "-");
            validationInfo.put("vaNumber", validation.getVirtualAccountNumber() != null ? validation.getVirtualAccountNumber() : "-");
            validationInfo.put("paymentAmount", validation.getPaymentAmount() != null ? validation.getPaymentAmount() : "-");
            validationInfo.put("createdAt", validation.getCreatedAt() != null ? validation.getCreatedAt() : "-");
            validationInfo.put("submittedAt", form.getSubmittedAt() != null ? form.getSubmittedAt() : "-");
            details.put("validation", validationInfo);
            
            // ===== WAVE TYPE (JALUR PENDAFTARAN) - ✅ FIXED: Get actual waveType from RegistrationPeriod =====
            String waveType = "REGULAR_TEST"; // Default
            if (form.getPeriod() != null && form.getPeriod().getWaveType() != null) {
                waveType = form.getPeriod().getWaveType().toString();
            }
            details.put("waveType", waveType);
            
            log.info("✅ Returning complete form details for validation {} - WaveType: {}", formValidationId, waveType);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("❌ Error fetching form details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get admission form student details - Wave Type, Selection Type, Program Studi
     * Used by validation modal to display additional info
     */
    @GetMapping("/api/admission-form/student/{studentId}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> getAdmissionFormStudentDetails(@PathVariable Long studentId) {
        try {
            log.info("📋 [ADMISSION-DETAILS] Fetching wave type, selection type, program studi for studentId: {}", studentId);
            
            // Find admission form for this student
            Optional<AdmissionForm> admissionFormOpt = admissionFormRepository.findAll()
                    .stream()
                    .filter(f -> f.getStudent() != null && f.getStudent().getId().equals(studentId))
                    .findFirst();
            
            if (admissionFormOpt.isEmpty()) {
                log.warn("⚠️ [ADMISSION-DETAILS] No admission form found for studentId: {}", studentId);
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Formulir pendaftaran tidak ditemukan"));
            }
            
            AdmissionForm form = admissionFormOpt.get();
            Map<String, Object> details = new HashMap<>();
            
            // ===== WAVE TYPE (from REGISTRATION_PERIODS) =====
            String waveType = "N/A";
            if (form.getPeriod() != null && form.getPeriod().getWaveType() != null) {
                waveType = form.getPeriod().getWaveType().toString();
                log.info("✅ [ADMISSION-DETAILS] Wave Type found: {}", waveType);
            } else {
                log.warn("⚠️ [ADMISSION-DETAILS] Wave Type not found for studentId: {}", studentId);
            }
            details.put("registrationPeriodWaveType", waveType);
            
            // ===== JENIS SELEKSI NAME (from JENIS_SELEKSI) =====
            String jenisSeleksiName = "N/A";
            if (form.getJenisSeleksiId() != null) {
                Optional<JenisSeleksi> jenisSeleksiOpt = jenisSeleksiRepository.findById(form.getJenisSeleksiId());
                if (jenisSeleksiOpt.isPresent()) {
                    jenisSeleksiName = jenisSeleksiOpt.get().getNama();
                    log.info("✅ [ADMISSION-DETAILS] Jenis Seleksi found: {}", jenisSeleksiName);
                } else {
                    log.warn("⚠️ [ADMISSION-DETAILS] Jenis Seleksi not found for ID: {}", form.getJenisSeleksiId());
                }
            } else {
                log.warn("⚠️ [ADMISSION-DETAILS] Jenis Seleksi ID is null for studentId: {}", studentId);
            }
            details.put("jenisSeleksiName", jenisSeleksiName);
            
            // ===== PROGRAM STUDI NAME (from CICILAN_REQUEST first, fallback to ADMISSION_FORMS) =====
            String programStudiName = "N/A";
            
            // First, try to get from CICILAN_REQUEST
            try {
                Optional<CicilanRequest> cicilanOpt = cicilanRequestRepository.findAll()
                        .stream()
                        .filter(c -> c.getStudent() != null && c.getStudent().getId().equals(studentId) && 
                                (c.getStatus().equals("APPROVED") || c.getStatus().equals("ACTIVE")))
                        .findFirst();
                
                if (cicilanOpt.isPresent()) {
                    CicilanRequest cicilan = cicilanOpt.get();
                    if (cicilan.getProgramStudi() != null) {
                        programStudiName = cicilan.getProgramStudi().getNama();
                        log.info("✅ [ADMISSION-DETAILS] Program Studi from CICILAN found: {}", programStudiName);
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [ADMISSION-DETAILS] Error fetching from CICILAN: {}", e.getMessage());
            }
            
            // Fallback to ADMISSION_FORMS first preference if not found in CICILAN
            if (programStudiName.equals("N/A") && form.getProgramStudi1() != null && !form.getProgramStudi1().isEmpty()) {
                programStudiName = form.getProgramStudi1();
                log.info("✅ [ADMISSION-DETAILS] Program Studi from ADMISSION_FORMS fallback: {}", programStudiName);
            }
            
            details.put("programStudiName", programStudiName);
            
            log.info("✅ [ADMISSION-DETAILS] Complete details: Wave={}, Seleksi={}, Prodi={}", 
                    waveType, jenisSeleksiName, programStudiName);
            
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            log.error("❌ [ADMISSION-DETAILS] Error fetching admission form details: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Approve form validation
     */
    @PutMapping("/api/validasi/formulir/{validationId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> approveFormValidation(@PathVariable Long validationId) {
        try {
            FormValidation validation = formValidationRepository.findById(validationId)
                    .orElseThrow(() -> new RuntimeException("Validasi tidak ditemukan"));

            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            validation.setValidationStatus(FormValidation.ValidationStatus.APPROVED);
            validation.setValidatedBy(admin);
            validation.setValidatedAt(LocalDateTime.now());
            formValidationRepository.save(validation);

            // Update form/student status
            AdmissionForm form = validation.getAdmissionForm();
            if (form != null) {
                form.setStatus(AdmissionForm.FormStatus.VERIFIED);
                admissionFormRepository.save(form);
                
                // UPDATE VALIDATION STATUS TRACKER TO DIVALIDASI
                try {
                    validationStatusTrackerService.updateStatusToDivalidasi(form.getId(), admin);
                    log.info("✅ ValidationStatusTracker updated to DIVALIDASI for form ID: {}", form.getId());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to update ValidationStatusTracker: {}", e.getMessage());
                }
                
                // ===== NEW: Update RegistrationStatus to DAFTAR_ULANG stage =====
                // This allows student to see re-enrollment page instead of validation waiting
                if (form.getStudent() != null) {
                    User student = form.getStudent().getUser();
                    if (student != null) {
                        RegistrationStatus reenrollStatus = registrationStatusService.getOrCreateStatus(
                                student, 
                                RegistrationStatus.RegistrationStage.DAFTAR_ULANG
                        );
                        // Ensure status is MENUNGGU_VERIFIKASI (waiting) so student can fill it
                        if (reenrollStatus.getStatus() == RegistrationStatus.RegistrationStatus_Enum.SELESAI ||
                            reenrollStatus.getStatus() == RegistrationStatus.RegistrationStatus_Enum.REJECTED) {
                            // If already completed, just update the timestamp
                            reenrollStatus.setUpdatedAt(LocalDateTime.now());
                        } else {
                            reenrollStatus.setStatus(RegistrationStatus.RegistrationStatus_Enum.MENUNGGU_VERIFIKASI);
                        }
                        registrationStatusRepository.save(reenrollStatus);
                        log.info("✅ Updated registration status to DAFTAR_ULANG for student: {} (Form ID: {})", student.getId(), form.getId());
                        
                        // ===== NEW: Auto-populate hasil akhir when validation is approved =====
                        try {
                            hasilAkhirService.autoPopulateHasilAkhir(form.getStudent().getId());
                            log.info("✅ [HASIL-AKHIR] Auto-populated hasil akhir for student: {}", form.getStudent().getId());
                        } catch (Exception e) {
                            log.warn("⚠️ [HASIL-AKHIR] Failed to auto-populate hasil akhir: {}", e.getMessage());
                            // Don't fail the approval process even if hasil akhir auto-population fails
                        }
                        
                        // ===== SEND APPROVAL EMAIL =====
                        sendApprovalEmail(student.getEmail(), form.getStudent().getFullName());
                    }
                }
            }

            return ResponseEntity.ok(new ApiResponse(true, "Formulir disetujui"));
        } catch (Exception e) {
            log.error("Error approving form validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW ENDPOINT: Save registration number and BRIVA to hasil-akhir table
     * PUT /admin/api/hasil-akhir/nomor-registrasi/{formValidationId}
     * Body: { nomorRegistrasi: "REG-...", brivaNumber: "..." }
     */
    @PutMapping("/api/hasil-akhir/nomor-registrasi/{formValidationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> updateHasilAkhirRegistrationNumber(
            @PathVariable Long formValidationId,
            @RequestBody HasilAkhirRegistrationRequest request) {
        try {
            // 🔍 DEBUG: Log the deserialized request object
            log.info("🔍 [HASIL-AKHIR-REQUEST] Received request - nomorRegistrasi: '{}' | brivaNumber: '{}' | brivaNumber_isNull: {}", 
                    request.getNomorRegistrasi(), 
                    request.getBrivaNumber(),
                    request.getBrivaNumber() == null);
            
            // Get form validation
            FormValidation validation = formValidationRepository.findById(formValidationId)
                    .orElseThrow(() -> new RuntimeException("Form validation tidak ditemukan"));

            // Get admission form and student
            AdmissionForm form = validation.getAdmissionForm();
            if (form == null || form.getStudent() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Student atau form tidak ditemukan"));
            }

            // Get or create hasil akhir
            Long studentId = form.getStudent().getId();
            Optional<HasilAkhir> hasilAkhirOpt = hasilAkhirRepository.findByStudent(form.getStudent());
            
            HasilAkhir hasilAkhir;
            if (hasilAkhirOpt.isPresent()) {
                hasilAkhir = hasilAkhirOpt.get();
                log.info("✅ [HASIL-AKHIR] Found existing HasilAkhir for student: {}", studentId);
            } else {
                // Create new hasil akhir if not exists
                hasilAkhir = new HasilAkhir();
                hasilAkhir.setStudent(form.getStudent());
                hasilAkhir.setUser(form.getStudent().getUser());
                log.info("✅ [HASIL-AKHIR] Creating new HasilAkhir for student: {}", studentId);
            }

            // Update registration number
            if (request.getNomorRegistrasi() != null && !request.getNomorRegistrasi().isEmpty()) {
                hasilAkhir.setNomorRegistrasi(request.getNomorRegistrasi());
                log.info("✅ [HASIL-AKHIR] Updated nomor registrasi: {}", request.getNomorRegistrasi());
            } else if (hasilAkhir.getNomorRegistrasi() == null || hasilAkhir.getNomorRegistrasi().isEmpty()) {
                // Auto-generate registration number to satisfy NOT NULL constraint
                String autoReg = "REG-" + java.time.LocalDate.now().toString().replace("-", "") + "-" + String.format("%06d", formValidationId);
                hasilAkhir.setNomorRegistrasi(autoReg);
                log.info("✅ [HASIL-AKHIR] Auto-generated nomor registrasi: {}", autoReg);
            }

            // ✅ DIRECT BRIVA SAVE: No complex logic, just save what frontend sends
            String brivaNumberToSave = request.getBrivaNumber();
            
            // 🔍 DEBUG: Log the exact value received
            log.info("🔍 [HASIL-AKHIR-DEBUG] brivaNumberToSave value: '{}' | isNull: {} | isEmpty: {} | length: {}", 
                    brivaNumberToSave, 
                    brivaNumberToSave == null, 
                    brivaNumberToSave != null && brivaNumberToSave.isEmpty(),
                    brivaNumberToSave != null ? brivaNumberToSave.length() : "N/A");
            
            if (brivaNumberToSave != null && !brivaNumberToSave.trim().isEmpty() && !brivaNumberToSave.equals("(Belum ada BRIVA)")) {
                // ✅ Frontend sent valid BRIVA, save it directly
                hasilAkhir.setBrivaNumber(brivaNumberToSave.trim());
                log.info("✅ [HASIL-AKHIR] BRIVA number WILL BE saved: {}", brivaNumberToSave.trim());
            } else {
                // ⚠️ BRIVA is empty, use timestamp-based placeholder to avoid NULL constraint
                if (hasilAkhir.getBrivaNumber() == null || hasilAkhir.getBrivaNumber().equals("N/A")) {
                    String placeholderBriva = "PENDING_" + System.currentTimeMillis();
                    hasilAkhir.setBrivaNumber(placeholderBriva);
                    log.warn("⚠️ [HASIL-AKHIR] BRIVA is empty, using placeholder: {}", placeholderBriva);
                }
            }
            
            // ✅ NEW: Handle jumlah cicilan from frontend
            if (request.getJumlahCicilan() != null && request.getJumlahCicilan() > 0) {
                hasilAkhir.setJumlahCicilan(request.getJumlahCicilan());
                log.info("✅ [HASIL-AKHIR] Jumlah cicilan saved: {}", request.getJumlahCicilan());
            } else if (hasilAkhir.getJumlahCicilan() == null) {
                hasilAkhir.setJumlahCicilan(1); // Default to 1 if not set
                log.info("✅ [HASIL-AKHIR] Jumlah cicilan set to default: 1");
            }

            // Save to database
            HasilAkhir saved = hasilAkhirRepository.save(hasilAkhir);
            
            // 🔍 DEBUG: Log what was actually saved to database
            log.info("🔍 [HASIL-AKHIR-VERIFY] After save - BRIVA in object: '{}' | isNull: {} | isEmpty: {} | length: {}", 
                    saved.getBrivaNumber(),
                    saved.getBrivaNumber() == null,
                    saved.getBrivaNumber() != null && saved.getBrivaNumber().isEmpty(),
                    saved.getBrivaNumber() != null ? saved.getBrivaNumber().length() : "N/A");
            
            log.info("✅ [HASIL-AKHIR] HasilAkhir saved successfully - ID: {}, BRIVA: {}, RegisterNumber: {}",
                    saved.getId(), saved.getBrivaNumber(), saved.getNomorRegistrasi());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nomor registrasi dan BRIVA berhasil disimpan");
            response.put("hasilAkhirId", saved.getId());
            return ResponseEntity.ok(response);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // ✅ Handle unique constraint violation
            log.error("❌ [HASIL-AKHIR] Unique constraint violation: {}", e.getMessage());
            String msg = "Gagal menyimpan: ";
            if (e.getMessage().contains("briva")) {
                msg += "Nomor BRIVA sudah terdaftar atau tidak valid";
            } else if (e.getMessage().contains("nomor_registrasi")) {
                msg += "Nomor Registrasi sudah terdaftar";
            } else {
                msg += "Data duplikat atau constraint violation";
            }
            return ResponseEntity.badRequest().body(new ApiResponse(false, msg));
        } catch (Exception e) {
            log.error("❌ [HASIL-AKHIR] Error updating hasil akhir: {}", e.getMessage(), e);
            log.error("❌ [HASIL-AKHIR] Full stack trace:", e);
            
            // Return detailed error for debugging
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Gagal menyimpan: " + e.getMessage());
            errorResponse.put("exceptionType", e.getClass().getSimpleName());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Reject form validation
     */
    @PutMapping("/api/validasi/formulir/{validationId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> rejectFormValidation(@PathVariable Long validationId, 
                                                   @RequestBody(required = false) FormValidationRejectRequest request) {
        try {
            FormValidation validation = formValidationRepository.findById(validationId)
                    .orElseThrow(() -> new RuntimeException("Validasi tidak ditemukan"));

            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            validation.setValidationStatus(FormValidation.ValidationStatus.REJECTED);
            
            // Handle optional request body
            String rejectionReason = "";
            if (request != null) {
                validation.setRejectionTopic(request.getTopic() != null ? request.getTopic() : "Lainnya");
                validation.setRejectionReason(request.getReason() != null ? request.getReason() : "");
                rejectionReason = request.getReason() != null ? request.getReason() : "";
            } else {
                validation.setRejectionTopic("Lainnya");
                validation.setRejectionReason("");
            }
            
            validation.setValidatedBy(admin);
            validation.setRejectedAt(LocalDateTime.now());
            formValidationRepository.save(validation);

            // Update form status
            AdmissionForm form = validation.getAdmissionForm();
            if (form != null) {
                form.setStatus(AdmissionForm.FormStatus.REJECTED);
                admissionFormRepository.save(form);
                
                // UPDATE VALIDATION STATUS TRACKER TO DITOLAK
                try {
                    validationStatusTrackerService.updateStatusToDitolak(form.getId(), rejectionReason, admin);
                    log.info("✅ ValidationStatusTracker updated to DITOLAK for form ID: {}", form.getId());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to update ValidationStatusTracker: {}", e.getMessage());
                }
                
                // ===== SEND REJECTION EMAIL =====
                if (form.getStudent() != null && form.getStudent().getUser() != null) {
                    String emailReason = validation.getRejectionReason() != null ? validation.getRejectionReason() : "Formulir Anda tidak memenuhi persyaratan";
                    sendRejectionEmail(form.getStudent().getUser().getEmail(), form.getStudent().getFullName(), emailReason);
                }
            }

            return ResponseEntity.ok(new ApiResponse(true, "Formulir ditolak"));
        } catch (Exception e) {
            log.error("Error rejecting form validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Mark form validation as needing revision (REVISION_NEEDED status)
     * Student can edit and resubmit the form
     */
    @PutMapping("/api/validasi/formulir/{validationId}/revision-needed")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> markFormAsRevisionNeeded(@PathVariable Long validationId,
                                                       @RequestBody(required = false) FormValidationRejectRequest request) {
        try {
            FormValidation validation = formValidationRepository.findById(validationId)
                    .orElseThrow(() -> new RuntimeException("Validasi tidak ditemukan"));

            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            validation.setValidationStatus(FormValidation.ValidationStatus.REVISION_NEEDED);
            
            // Handle optional request body
            String revisionReason = "";
            Integer revisionNumber = 1;
            if (request != null) {
                validation.setRejectionTopic(request.getTopic() != null ? request.getTopic() : "Perbaiki Data");
                validation.setRejectionReason(request.getReason() != null ? request.getReason() : "");
                revisionReason = request.getReason() != null ? request.getReason() : "";
                revisionNumber = request.getRevisionNumber() != null ? request.getRevisionNumber() : 1;
                validation.setRevisionNumber(revisionNumber);
            } else {
                validation.setRejectionTopic("Perbaiki Data");
                validation.setRejectionReason("");
                validation.setRevisionNumber(1);
            }
            
            validation.setValidatedBy(admin);
            validation.setValidatedAt(LocalDateTime.now());
            formValidationRepository.save(validation);

            // ✅ NEW: Reset FormRepairStatus back to BELUM_PERBAIKAN when requesting new revision
            // (because previous repair wasn't good enough, student needs to fix again)
            try {
                Optional<FormRepairStatus> repairStatusOpt = formRepairStatusRepository.findByFormValidationId(validationId);
                if (repairStatusOpt.isPresent()) {
                    FormRepairStatus repairStatus = repairStatusOpt.get();
                    repairStatus.setStatus(FormRepairStatus.RepairStatus.BELUM_PERBAIKAN);
                    repairStatus.setUpdatedAt(LocalDateTime.now());
                    formRepairStatusRepository.save(repairStatus);
                    log.info("✅ FormRepairStatus reset to BELUM_PERBAIKAN for validation ID: {}", validationId);
                } else {
                    log.warn("⚠️ FormRepairStatus not found for validation ID: {}", validationId);
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to reset FormRepairStatus: {}", e.getMessage());
            }

            // Keep form as SUBMITTED so student can edit
            AdmissionForm form = validation.getAdmissionForm();
            if (form != null) {
                form.setStatus(AdmissionForm.FormStatus.SUBMITTED);
                admissionFormRepository.save(form);
                
                // UPDATE VALIDATION STATUS TRACKER TO REVISI
                try {
                    validationStatusTrackerService.updateStatusToRevisi(form.getId(), revisionReason, admin);
                    log.info("✅ ValidationStatusTracker updated to REVISI for form ID: {}", form.getId());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to update ValidationStatusTracker: {}", e.getMessage());
                }
                
                // ===== SEND REVISION-NEEDED EMAIL =====
                if (form.getStudent() != null && form.getStudent().getUser() != null) {
                    String emailRevisionReason = validation.getRejectionReason() != null ? validation.getRejectionReason() : "Ada beberapa data yang perlu diperbaiki";
                    sendRevisionNeededEmail(form.getStudent().getUser().getEmail(), form.getStudent().getFullName(), emailRevisionReason, revisionNumber);
                }
            }

            return ResponseEntity.ok(new ApiResponse(true, "Formulir ditandai untuk revisi"));
        } catch (Exception e) {
            log.error("Error marking form as revision needed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Update repair status (mark revision as complete)
     * PUT /admin/api/form-validations/student/{studentId}/repair-status
     * Called when student marks revision as complete
     */
    @PutMapping("/api/form-validations/student/{studentId}/repair-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateRepairStatus(
            @PathVariable Long studentId,
            @RequestBody Map<String, String> request) {
        try {
            log.info("🔧 [REPAIR-STATUS] Updating repair status for student ID: {}", studentId);
            
            String repairStatus = request.get("repairStatus");
            log.info("   Repair Status: {}", repairStatus);
            
            // Find student by ID
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student tidak ditemukan"));
            
            log.info("   Student Found: {}", student.getFullName());
            
            // Find latest FormValidation for this student
            java.util.List<FormValidation> validations = formValidationRepository.findAll().stream()
                    .filter(fv -> fv.getStudent().getId().equals(studentId))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
            
            if (validations.isEmpty()) {
                log.warn("⚠️ No FormValidation found for student: {}", studentId);
                return ResponseEntity.ok(new ApiResponse(true, "Repair status tidak perlu di-update (no validation record)"));
            }
            
            FormValidation validation = validations.get(0); // Get latest
            log.info("   FormValidation ID: {}", validation.getId());
            log.info("   Current Status: {}", validation.getValidationStatus());
            
            // ✅ NEW: Update FormRepairStatus record (separate table)
            java.util.Optional<FormRepairStatus> repairStatusOptional = formRepairStatusRepository.findByFormValidationId(validation.getId());
            
            FormRepairStatus repairStatusRecord;
            if (repairStatusOptional.isPresent()) {
                repairStatusRecord = repairStatusOptional.get();
                log.info("✅ Found existing FormRepairStatus record: {}", repairStatusRecord.getId());
            } else {
                // Create new repairStatus record if it doesn't exist
                repairStatusRecord = FormRepairStatus.builder()
                        .formValidation(validation)
                        .status(FormRepairStatus.RepairStatus.BELUM_PERBAIKAN)
                        .createdAt(LocalDateTime.now())
                        .build();
                log.info("✅ Creating new FormRepairStatus record for validation: {}", validation.getId());
            }
            
            // Update repair status
            if ("SUDAH_PERBAIKAN".equals(repairStatus)) {
                repairStatusRecord.setStatus(FormRepairStatus.RepairStatus.SUDAH_PERBAIKAN);
                repairStatusRecord.setUpdatedAt(LocalDateTime.now());
                formRepairStatusRepository.save(repairStatusRecord);
                log.info("✅ Repair status updated to SUDAH_PERBAIKAN for FormRepairStatus ID: {}", repairStatusRecord.getId());
            } else if ("BELUM_PERBAIKAN".equals(repairStatus)) {
                repairStatusRecord.setStatus(FormRepairStatus.RepairStatus.BELUM_PERBAIKAN);
                repairStatusRecord.setUpdatedAt(LocalDateTime.now());
                formRepairStatusRepository.save(repairStatusRecord);
                log.info("✅ Repair status reset to BELUM_PERBAIKAN for FormRepairStatus ID: {}", repairStatusRecord.getId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Repair status berhasil diperbarui");
            response.put("studentId", studentId);
            response.put("repairStatus", repairStatus);
            response.put("validationId", validation.getId());
            response.put("repairStatusRecordId", repairStatusRecord.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [REPAIR-STATUS] Error updating repair status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get ValidationStatusTracker by form ID
     * GET /admin/api/validation-status-tracker/form/{formId}
     * Returns the current validation status for a form
     */
    @GetMapping("/api/validation-status-tracker/form/{formId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getValidationStatusTrackerByFormId(@PathVariable Long formId) {
        try {
            log.info("📊 Getting ValidationStatusTracker for form ID: {}", formId);
            
            ValidationStatusTracker tracker = validationStatusTrackerService.getTrackerByFormId(formId)
                    .orElse(null);
            
            if (tracker == null) {
                log.warn("⚠️ No ValidationStatusTracker found for form ID: {}", formId);
                return ResponseEntity.ok(new ApiResponse(false, "Tracker not found for form"));
            }
            
            // Build response DTO
            Map<String, Object> response = new HashMap<>();
            response.put("id", tracker.getId());
            response.put("formId", tracker.getAdmissionForm().getId());
            response.put("studentId", tracker.getStudent().getId());
            response.put("studentName", tracker.getStudent().getFullName());
            response.put("studentEmail", tracker.getStudent().getUser().getEmail());
            response.put("status", tracker.getStatus().toString());
            response.put("lastReason", tracker.getLastReason());
            response.put("lastAction", tracker.getLastAction());
            response.put("createdAt", tracker.getCreatedAt());
            response.put("updatedAt", tracker.getUpdatedAt());
            
            log.info("✅ Returning ValidationStatusTracker - Status: {} for form {}", tracker.getStatus(), formId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching ValidationStatusTracker: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get ValidationStatusTracker by student ID
     * GET /admin/api/validation-status-tracker/student/{studentId}
     * Returns the current validation status for a student
     */
    @GetMapping("/api/validation-status-tracker/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getValidationStatusTrackerByStudentId(@PathVariable Long studentId) {
        try {
            log.info("📊 Getting ValidationStatusTracker for student ID: {}", studentId);
            
            ValidationStatusTracker tracker = validationStatusTrackerService.getTrackerByStudentId(studentId)
                    .orElse(null);
            
            if (tracker == null) {
                log.warn("⚠️ No ValidationStatusTracker found for student ID: {}", studentId);
                return ResponseEntity.ok(new ApiResponse(false, "Tracker not found for student"));
            }
            
            // Build response DTO
            Map<String, Object> response = new HashMap<>();
            response.put("id", tracker.getId());
            response.put("formId", tracker.getAdmissionForm().getId());
            response.put("studentId", tracker.getStudent().getId());
            response.put("studentName", tracker.getStudent().getFullName());
            response.put("studentEmail", tracker.getStudent().getUser().getEmail());
            response.put("status", tracker.getStatus().toString());
            response.put("lastReason", tracker.getLastReason());
            response.put("lastAction", tracker.getLastAction());
            response.put("createdAt", tracker.getCreatedAt());
            response.put("updatedAt", tracker.getUpdatedAt());
            
            log.info("✅ Returning ValidationStatusTracker - Status: {} for student {}", tracker.getStatus(), studentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching ValidationStatusTracker: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== VALIDASI DATA DAFTAR ULANG ==========

    /**
     * Get re-enrollments untuk validasi
     */
    @GetMapping("/api/validasi/daftar-ulang")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReEnrollmentsForValidation() {
        try {
            List<ReEnrollmentValidation> validations = reEnrollmentValidationRepository
                    .findByValidationStatusOrderByCreatedAtDesc(ReEnrollmentValidation.ValidationStatus.PENDING);
            
            List<Map<String, Object>> response = validations.stream().map(rv -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", rv.getId());
                data.put("studentId", rv.getStudent().getId());
                data.put("studentName", rv.getStudent().getFullName());
                data.put("studentEmail", rv.getStudent().getUser().getEmail());
                data.put("reEnrollmentId", rv.getReEnrollment().getId());
                data.put("validationStatus", rv.getValidationStatus().toString());
                data.put("createdAt", rv.getCreatedAt());
                return data;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching reenrollments for validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Approve re-enrollment validation
     */
    @PutMapping("/api/validasi/daftar-ulang/{validationId}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> approveReEnrollmentValidation(@PathVariable Long validationId) {
        try {
            ReEnrollmentValidation validation = reEnrollmentValidationRepository.findById(validationId)
                    .orElseThrow(() -> new RuntimeException("Validasi tidak ditemukan"));

            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            validation.setValidationStatus(ReEnrollmentValidation.ValidationStatus.APPROVED);
            validation.setValidatedBy(admin);
            validation.setValidatedAt(LocalDateTime.now());
            reEnrollmentValidationRepository.save(validation);

            return ResponseEntity.ok(new ApiResponse(true, "Daftar ulang disetujui"));
        } catch (Exception e) {
            log.error("Error approving reenrollment validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Reject re-enrollment validation
     */
    @PutMapping("/api/validasi/daftar-ulang/{validationId}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rejectReEnrollmentValidation(@PathVariable Long validationId,
                                                          @RequestBody(required = false) FormValidationRejectRequest request) {
        try {
            ReEnrollmentValidation validation = reEnrollmentValidationRepository.findById(validationId)
                    .orElseThrow(() -> new RuntimeException("Validasi tidak ditemukan"));

            // Get current admin
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User admin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            validation.setValidationStatus(ReEnrollmentValidation.ValidationStatus.REJECTED);
            
            // Handle optional request body
            if (request != null) {
                validation.setRejectionTopic(request.getTopic() != null ? request.getTopic() : "Lainnya");
                validation.setRejectionReason(request.getReason() != null ? request.getReason() : "");
            } else {
                validation.setRejectionTopic("Lainnya");
                validation.setRejectionReason("");
            }
            
            validation.setValidatedBy(admin);
            validation.setRejectedAt(LocalDateTime.now());
            reEnrollmentValidationRepository.save(validation);

            return ResponseEntity.ok(new ApiResponse(true, "Daftar ulang ditolak"));
        } catch (Exception e) {
            log.error("Error rejecting reenrollment validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== EMAIL NOTIFICATION HELPER METHODS ==========

    /**
     * Send approval email to student
     */
    private void sendApprovalEmail(String studentEmail, String studentName) {
        try {
            String subject = "🎓 SELAMAT! Pendaftaran Anda Telah Disetujui - Lihat Nomor Registrasi & Virtual Account";
            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px;">
                            <div style="text-align: center; padding: 20px; background-color: #28a745; border-radius: 8px 8px 0 0; color: white;">
                                <h1 style="margin: 0; font-size: 32px;">🎉 SELAMAT!</h1>
                                <p style="margin: 10px 0 0 0; font-size: 18px;">Anda Telah Menyelesaikan Pendaftaran</p>
                            </div>
                            
                            <div style="padding: 30px; background-color: white;">
                                <p>Dear <strong>%s</strong>,</p>
                                
                                <p>Kami dengan senang hati memberitahukan bahwa formulir pendaftaran Anda telah <strong>DISETUJUI ✅</strong> oleh tim verifikasi kami. Selamat! Anda telah resmi menyelesaikan tahap pendaftaran.</p>
                                
                                <div style="background-color: #e8f5e9; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h3 style="margin-top: 0; color: #28a745;">📍 DATA PENTING ANDA:</h3>
                                    <p style="margin: 5px 0; color: #1b5e20;"><strong>Nomor Registrasi Akademik:</strong> Dapat dilihat di dashboard (tombol "Hasil Akhir Penerimaan")</p>
                                    <p style="margin: 5px 0; color: #1b5e20;"><strong>Virtual Account BRIVA:</strong> Dapat dilihat di dashboard (tombol "Hasil Akhir Penerimaan")</p>
                                    <p style="margin: 5px 0; color: #1b5e20;"><strong>📌 PENTING:</strong> Simpan kedua data ini untuk referensi Anda ke depannya.</p>
                                </div>
                                
                                <h3 style="color: #1976d2;">🎯 Langkah Berikutnya:</h3>
                                <ol style="color: #555;">
                                    <li><strong>Login ke Dashboard PMB</strong> - Gunakan akun yang sudah didaftarkan</li>
                                    <li><strong>Lihat "Hasil Akhir Penerimaan"</strong> - Tombol hijau di dashboard menampilkan nomor registrasi & virtual account</li>
                                    <li><strong>Catat Nomor Registrasi & Virtual Account</strong> - Gunakan untuk keperluan akademik Anda</li>
                                    <li><strong>Siapkan Dokumen</strong> - Kumpulkan dokumen-dokumen yang diperlukan untuk tahap selanjutnya</li>
                                    <li><strong>Tunggu Pengumuman Jadwal Berikutnya</strong> - Kami akan mengirimkan informasi lebih lanjut melalui email</li>
                                </ol>
                                
                                <div style="background-color: #e3f2fd; border-left: 4px solid #1976d2; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h4 style="margin-top: 0; color: #0d47a1;">📲 Akses "Hasil Akhir Penerimaan":</h4>
                                    <ul style="margin: 10px 0; padding-left: 20px; color: #1565c0;">
                                        <li>Login ke dashboard PMB Anda</li>
                                        <li>Cari tombol <strong>"📄 Hasil Akhir Penerimaan"</strong> (warna hijau)</li>
                                        <li>Klik untuk melihat detail lengkap: Nomor Registrasi, Virtual Account BRIVA, dan status pendaftaran</li>
                                    </ul>
                                </div>
                                
                                <div style="background-color: #fff3cd; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <p style="margin: 0; color: #e65100;"><strong>⚠️ PENTING:</strong> Jangan hilangkan nomor registrasi dan virtual account Anda. Data ini diperlukan untuk komunikasi resmi dengan universitas.</p>
                                </div>
                                
                                <p style="color: #666; margin-top: 30px;">Terima kasih telah mempercayai HKBP Nommensen. Kami menantikan Anda sebagai bagian dari mahasiswa kami.</p>
                                
                                <p style="color: #666; margin: 10px 0;">Jika ada pertanyaan, silakan hubungi tim PMB melalui portal atau email customer service kami.</p>
                                
                                <p style="margin-top: 30px; color: #999; font-size: 12px;">---<br/>Tim PMB<br/>HKBP Nommensen<br/>Universitas HKBP Nommensen</p>
                            </div>
                        </div>
                    </body>
                </html>
                """, studentName);
            
            emailService.sendHtmlEmail(studentEmail, subject, htmlContent);
            log.info("✅ Approval email sent to: {}", studentEmail);
        } catch (Exception e) {
            log.error("❌ Error sending approval email to {}: {}", studentEmail, e.getMessage());
        }
    }

    /**
     * Send rejection email to student (permanent rejection)
     */
    private void sendRejectionEmail(String studentEmail, String studentName, String rejectionReason) {
        try {
            String subject = "❌ Formulir Pendaftaran Anda Ditolak - PMB HKBP Nommensen";
            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px;">
                            <div style="text-align: center; padding: 20px; background-color: #dc3545; border-radius: 8px 8px 0 0; color: white;">
                                <h1 style="margin: 0; font-size: 28px;">❌ PEMBERITAHUAN PENOLAKAN</h1>
                                <p style="margin: 10px 0 0 0; font-size: 14px;">Formulir Pendaftaran Tidak Diterima</p>
                            </div>
                            
                            <div style="padding: 30px; background-color: white;">
                                <p>Dear <strong>%s</strong>,</p>
                                
                                <p>Terima kasih atas kepercayaan dan minat Anda untuk bergabung dengan HKBP Nommensen. Setelah melakukan review menyeluruh dan komprehensif terhadap formulir pendaftaran Anda, kami dengan menyesal memberitahukan bahwa <strong>FORMULIR PENDAFTARAN ANDA TIDAK DAPAT DITERIMA PADA GELOMBANG INI</strong>.</p>
                                
                                <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h3 style="margin-top: 0; color: #721c24;">📋 Alasan Penolakan:</h3>
                                    <p style="margin: 5px 0; color: #721c24; font-size: 15px; line-height: 1.8;"><strong>%s</strong></p>
                                </div>
                                
                                <h3 style="color: #1976d2;">📌 Informasi Penting Untuk Anda:</h3>
                                <ul style="color: #555; line-height: 1.9;">
                                    <li><strong>Penolakan ini bersifat final</strong> untuk gelombang pendaftaran tahun ini dan tidak dapat diajukan banding</li>
                                    <li><strong>Kesempatan Berikutnya:</strong> Anda dapat mendaftar kembali di gelombang pendaftaran berikutnya jika tersedia (dengan syarat dan ketentuan yang berlaku)</li>
                                    <li><strong>Pembayaran:</strong> Jika Anda sudah melakukan pembayaran, silakan hubungi tim PMB kami untuk proses refund</li>
                                    <li><strong>Dokumen dan Data:</strong> Data pendaftaran Anda akan disimpan dalam sistem sebagai arsip</li>
                                </ul>
                                
                                <div style="background-color: #fff3cd; border-left: 4px solid #f57f17; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h4 style="margin-top: 0; color: #e65100;">💼 Saran Untuk Masa Depan:</h4>
                                    <p style="margin: 5px 0; color: #bf360c;">• Tingkatkan persiapan dan kualitas dokumen pendukung Anda</p>
                                    <p style="margin: 5px 0; color: #bf360c;">• Pastikan semua data yang Anda masukkan akurat dan lengkap</p>
                                    <p style="margin: 5px 0; color: #bf360c;">• Ikuti setiap panduan dan instruksi dengan seksama pada pendaftaran berikutnya</p>
                                </div>
                                
                                <div style="background-color: #e3f2fd; border-left: 4px solid #1976d2; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h4 style="margin-top: 0; color: #0d47a1;">📞 Konsultasi Lebih Lanjut:</h4>
                                    <p style="margin: 5px 0; color: #1565c0;">Jika Anda ingin mendisuksikan hasil penolakan ini secara lebih detail, Anda dapat:</p>
                                    <ul style="margin: 10px 0; padding-left: 20px; color: #1565c0;">
                                        <li>Hubungi tim Customer Service PMB melalui portal</li>
                                        <li>Mengirimkan email konsultasi ke email PMB resmi kami</li>
                                        <li>Mengunjungi kantor PMB selama jam kerja (dengan perjanjian terlebih dahulu)</li>
                                    </ul>
                                </div>
                                
                                <p style="color: #666; margin-top: 30px;">Kami mengucapkan terima kasih atas waktu, perhatian, dan upaya Anda dalam proses seleksi kami. Kami juga menghargai kepercayaan Anda kepada HKBP Nommensen dan berharap dapat bertemu dengan Anda di kesempatan pendaftaran berikutnya.</p>
                                
                                <p style="color: #666; margin: 15px 0 0 0;">Semangat dan terus berkembang untuk masa depan yang lebih baik!</p>
                                
                                <p style="margin-top: 30px; color: #999; font-size: 12px;">---<br/>Tim Penerimaan Mahasiswa (PMB)<br/>HKBP Nommensen<br/>Universitas HKBP Nommensen</p>
                            </div>
                        </div>
                    </body>
                </html>
                """, studentName, rejectionReason);
            
            emailService.sendHtmlEmail(studentEmail, subject, htmlContent);
            log.info("❌ Rejection email sent to: {}", studentEmail);
        } catch (Exception e) {
            log.error("❌ Error sending rejection email to {}: {}", studentEmail, e.getMessage());
        }
    }

    /**
     * Send revision-needed email to student (form requires revision)
     */
    private void sendRevisionNeededEmail(String studentEmail, String studentName, String revisionReason, Integer revisionNumber) {
        try {
            String subject = "✏️ REVISI DIPERLUKAN - Silakan Perbaiki Data Formulir Anda (Revisi ke-" + revisionNumber + ")";
            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px;">
                            <div style="text-align: center; padding: 20px; background-color: #ff9800; border-radius: 8px 8px 0 0; color: white;">
                                <h1 style="margin: 0; font-size: 28px;">✏️ REVISI DIPERLUKAN (Revisi ke-%d)</h1>
                                <p style="margin: 10px 0 0 0; font-size: 14px;">Silakan Perbaiki Data Formulir Anda</p>
                            </div>
                            
                            <div style="padding: 30px; background-color: white;">
                                <p>Dear <strong>%%s</strong>,</p>
                                
                                <p>Terima kasih telah mengajukan formulir pendaftaran. Tim verifikasi kami telah melakukan review menyeluruh terhadap formulir Anda dan menemukan beberapa data yang perlu Anda perbaiki sebelum dapat kami setujui.</p>
                                
                                <div style="background-color: #ffe0b2; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h3 style="margin-top: 0; color: #e65100;">📝 Data yang Perlu Diperbaiki (Revisi ke-%d):</h3>
                                    <p style="margin: 0; color: #bf360c; font-size: 15px; line-height: 1.8;"><strong>%%s</strong></p>
                                </div>
                                
                                <h3 style="color: #1976d2;">🔧 Cara Memperbaiki Formulir:</h3>
                                <ol style="color: #555; line-height: 2;">
                                    <li><strong>Login ke Dashboard PMB</strong> - Masuk dengan email dan password yang sudah terdaftar</li>
                                    <li><strong>Cari Formulir Anda</strong> - Buka tab "Dashboard" dan cari formulir dengan status <span style="background: #fff3cd; padding: 2px 6px; border-radius: 3px;">"Perlu Revisi"</span></li>
                                    <li><strong>Klik Tombol "Edit Sekarang"</strong> - Tombol berwarna hijau di sebelah formulir</li>
                                    <li><strong>Perbaiki Data Sesuai Feedback</strong> - Bacalah dengan seksama dan update semua field yang bermasalah</li>
                                    <li><strong>Simpan & Submit Ulang</strong> - Setelah mengubah data, klik tombol "Kirim Ulang" atau "Submit" untuk mengirimkan formulir yang sudah diperbaiki</li>
                                    <li><strong>Tunggu Notifikasi</strong> - Tim kami akan review ulang dalam waktu singkat. Anda akan menerima email notifikasi setelah review selesai</li>
                                </ol>
                                
                                <div style="background-color: #e8f5e9; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h4 style="margin-top: 0; color: #2e7d32;">💡 Tips Perbaikan:</h4>
                                    <ul style="margin: 10px 0; padding-left: 20px; color: #1b5e20;">
                                        <li>Bacalah feedback dengan seksama sebelum mulai edit</li>
                                        <li>Pastikan semua data sesuai dengan dokumen pendukung Anda</li>
                                        <li>Periksa kembali data sebelum submit untuk menghindari kesalahan</li>
                                        <li>Jika ada field yang tidak jelas, hubungi tim PMB untuk bantuan</li>
                                    </ul>
                                </div>
                                
                                <div style="background-color: #fff3cd; border-left: 4px solid #f57f17; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <p style="margin: 0; color: #e65100;"><strong>⚠️ PENTING:</strong> Silakan lakukan perbaikan dan submit ulang formulir <strong>secepatnya</strong>. Setiap hari penundaan dapat mempengaruhi jadwal proses pendaftaran Anda selanjutnya.</p>
                                </div>
                                
                                <div style="background-color: #f3e5f5; border-left: 4px solid #9c27b0; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h4 style="margin-top: 0; color: #6a1b9a;">🐛 Jika Formulir Tidak Bisa Diedit:</h4>
                                    <p style="margin: 5px 0; color: #4a148c;">1. Refresh halaman browser Anda</p>
                                    <p style="margin: 5px 0; color: #4a148c;">2. Logout dan login kembali</p>
                                    <p style="margin: 5px 0; color: #4a148c;">3. Jika masih bermasalah, hubungi tim customer service kami</p>
                                </div>
                                
                                <p style="color: #666; margin-top: 30px;">Kami menantikan formulir yang sudah diperbaiki dari Anda. Jika ada pertanyaan atau kesulitan, jangan ragu untuk menghubungi tim PMB kami melalui portal atau email.</p>
                                
                                <p style="margin: 15px 0 0 0; color: #999; font-size: 12px;">---<br/>Tim PMB<br/>HKBP Nommensen<br/>Universitas HKBP Nommensen</p>
                            </div>
                        </div>
                    </body>
                </html>
                """, revisionNumber, studentName, revisionNumber, revisionReason);
            
            emailService.sendHtmlEmail(studentEmail, subject, htmlContent);
            log.info("✏️ Revision-needed email sent to: {} (Revisi ke-{})", studentEmail, revisionNumber);
        } catch (Exception e) {
            log.error("❌ Error sending revision-needed email to {}: {}", studentEmail, e.getMessage());
        }
    }

    // ========== EXAM VALIDATION EMAIL NOTIFICATIONS ==========

    /**
     * Send email when exam is APPROVED
     */
    private void sendExamApprovalEmail(String studentEmail, String studentName) {
        try {
            String subject = "✅ Ujian Anda Telah Diterima - PMB HKBP Nommensen";
            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px;">
                            <div style="text-align: center; padding: 20px; background-color: #28a745; border-radius: 8px 8px 0 0; color: white;">
                                <h1 style="margin: 0; font-size: 28px;">✅ UJIAN DITERIMA</h1>
                                <p style="margin: 10px 0 0 0; font-size: 16px;">Selamat! Hasil Ujian Anda Telah Divalidasi</p>
                            </div>
                            
                            <div style="padding: 30px; background-color: white;">
                                <p>Dear <strong>%s</strong>,</p>
                                
                                <p>Kami dengan senang hati memberitahukan bahwa hasil ujian Anda telah <strong>DITERIMA ✅</strong> oleh tim verifikasi kami.</p>
                                
                                <div style="background-color: #e8f5e9; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h3 style="margin-top: 0; color: #28a745;">🎉 Status Ujian: DITERIMA</h3>
                                    <p style="margin: 0; color: #1b5e20;">Token dan bukti ujian Anda telah diverifikasi dan dinyatakan valid.</p>
                                </div>
                                
                                <h3 style="color: #1976d2;">📋 Langkah Selanjutnya:</h3>
                                <ol style="color: #555;">
                                    <li><strong>Login ke Dashboard PMB</strong> - Cek status terbaru di dashboard Anda</li>
                                    <li><strong>Lakukan Pembayaran Cicilan 1</strong> - Bayar cicilan pertama untuk melanjutkan ke tahap daftar ulang</li>
                                    <li><strong>Lengkapi Daftar Ulang</strong> - Setelah pembayaran, isi formulir daftar ulang</li>
                                </ol>
                                
                                <div style="background-color: #fff3cd; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <p style="margin: 0; color: #e65100;"><strong>⚠️ PENTING:</strong> Segera lakukan pembayaran cicilan 1 agar proses pendaftaran Anda tidak terhambat.</p>
                                </div>
                                
                                <p style="color: #666; margin-top: 30px;">Terima kasih dan selamat! Kami menantikan Anda sebagai bagian dari mahasiswa HKBP Nommensen.</p>
                                
                                <p style="margin-top: 30px; color: #999; font-size: 12px;">---<br/>Tim PMB<br/>HKBP Nommensen<br/>Universitas HKBP Nommensen</p>
                            </div>
                        </div>
                    </body>
                </html>
                """, studentName);

            emailService.sendHtmlEmail(studentEmail, subject, htmlContent);
            log.info("✅ Exam approval email sent to: {}", studentEmail);
        } catch (Exception e) {
            log.error("❌ Error sending exam approval email to {}: {}", studentEmail, e.getMessage());
        }
    }

    /**
     * Send email when exam is REJECTED
     */
    private void sendExamRejectionEmail(String studentEmail, String studentName, String rejectionReason) {
        try {
            String subject = "❌ Ujian Anda Ditolak - PMB HKBP Nommensen";
            String htmlContent = String.format("""
                <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px;">
                            <div style="text-align: center; padding: 20px; background-color: #dc3545; border-radius: 8px 8px 0 0; color: white;">
                                <h1 style="margin: 0; font-size: 28px;">❌ UJIAN DITOLAK</h1>
                                <p style="margin: 10px 0 0 0; font-size: 14px;">Hasil Ujian Tidak Dapat Diterima</p>
                            </div>
                            
                            <div style="padding: 30px; background-color: white;">
                                <p>Dear <strong>%s</strong>,</p>
                                
                                <p>Setelah melakukan review terhadap hasil ujian Anda, kami memberitahukan bahwa <strong>hasil ujian Anda TIDAK DAPAT DITERIMA</strong>.</p>
                                
                                <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h3 style="margin-top: 0; color: #721c24;">📝 Alasan Penolakan:</h3>
                                    <p style="margin: 0; color: #721c24;">%s</p>
                                </div>
                                
                                <div style="background-color: #e3f2fd; border-left: 4px solid #1976d2; padding: 15px; margin: 20px 0; border-radius: 4px;">
                                    <h4 style="margin-top: 0; color: #0d47a1;">💡 Apa yang bisa dilakukan?</h4>
                                    <ul style="margin: 10px 0; padding-left: 20px; color: #1565c0;">
                                        <li>Hubungi tim PMB untuk informasi lebih lanjut</li>
                                        <li>Periksa kembali dashboard Anda untuk detail lengkap</li>
                                    </ul>
                                </div>
                                
                                <p style="color: #666; margin-top: 30px;">Jika Anda merasa ada kesalahan atau memiliki pertanyaan, silakan hubungi tim PMB kami.</p>
                                
                                <p style="margin-top: 30px; color: #999; font-size: 12px;">---<br/>Tim PMB<br/>HKBP Nommensen<br/>Universitas HKBP Nommensen</p>
                            </div>
                        </div>
                    </body>
                </html>
                """, studentName, rejectionReason.isEmpty() ? "Tidak memenuhi kriteria validasi" : rejectionReason);

            emailService.sendHtmlEmail(studentEmail, subject, htmlContent);
            log.info("❌ Exam rejection email sent to: {}", studentEmail);
        } catch (Exception e) {
            log.error("❌ Error sending exam rejection email to {}: {}", studentEmail, e.getMessage());
        }
    }

    // ========== CS MESSAGING SYSTEM ==========

    /**
     * Get unread message count
     */
    @GetMapping("/api/messages/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getUnreadMessageCount() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            Long unreadCount = adminMessageRepository.countUnreadMessages(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting unread count: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Send message
     */
    @PostMapping("/api/messages/send")
    @PreAuthorize("hasAnyRole('ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User sender = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            User recipient = userRepository.findById(request.getRecipientId())
                    .orElseThrow(() -> new RuntimeException("Recipient tidak ditemukan"));

            AdminMessage message = AdminMessage.builder()
                    .sender(sender)
                    .recipient(recipient)
                    .messageContent(request.getMessageContent())
                    .messageType(request.getMessageType() != null ? request.getMessageType() : "QUESTION")
                    .admissionFormId(request.getAdmissionFormId())
                    .status(AdminMessage.MessageStatus.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            adminMessageRepository.save(message);
            return ResponseEntity.ok(new ApiResponse(true, "Pesan dikirim"));
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get conversation
     */
    @GetMapping("/api/messages/conversation/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getConversation(@PathVariable Long userId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            List<AdminMessage> messages = adminMessageRepository
                    .findConversationBetween(currentUser.getId(), userId);

            // Mark as read
            messages.forEach(msg -> {
                if (msg.getRecipient().getId().equals(currentUser.getId()) 
                    && msg.getStatus() == AdminMessage.MessageStatus.UNREAD) {
                    msg.setStatus(AdminMessage.MessageStatus.READ);
                    msg.setReadAt(LocalDateTime.now());
                    adminMessageRepository.save(msg);
                }
            });

            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error fetching conversation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all messages for current user - receives messages sent by students
     */
    @GetMapping("/api/messages")
    @PreAuthorize("hasAnyRole('ADMIN_VALIDASI', 'ADMIN_PUSAT', 'CAMABA')")
    public ResponseEntity<?> getMessages() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            log.info("🔍 [DEBUG] Getting messages for user: {} (ID: {})", userEmail, user.getId());

            // Get ALL messages - all admins see all messages (1 copy only, no duplicates)
            List<AdminMessage> messages = adminMessageRepository.findAll();
            
            log.info("📊 [DEBUG] Found {} messages for admin recipient {}", messages.size(), user.getId());
            
            // Map to DTO to avoid lazy loading issues
            List<Map<String, Object>> responseMsgs = new ArrayList<>();
            
            for (AdminMessage msg : messages) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                
                // Safe null checking for sender
                User sender = msg.getSender();
                Long senderId = sender != null ? sender.getId() : null;
                String senderEmail = sender != null ? sender.getEmail() : "Unknown";
                String senderType = (sender != null && sender.getRole() == User.UserRole.CAMABA) ? "STUDENT" : "ADMIN";
                User.UserRole senderRole = sender != null ? sender.getRole() : null;
                
                msgMap.put("senderId", senderId);
                msgMap.put("senderEmail", senderEmail);
                msgMap.put("senderType", senderType);
                msgMap.put("senderRole", senderRole);
                
                // Safe null checking for recipient
                User recipient = msg.getRecipient();
                Long recipientId = recipient != null ? recipient.getId() : null;
                String recipientEmail = recipient != null ? recipient.getEmail() : "Unknown";
                msgMap.put("recipientId", recipientId);
                msgMap.put("recipientEmail", recipientEmail);
                
                msgMap.put("messageContent", msg.getMessageContent());
                msgMap.put("messageType", msg.getMessageType());
                msgMap.put("status", msg.getStatus().toString());
                msgMap.put("createdAt", msg.getCreatedAt());
                msgMap.put("readAt", msg.getReadAt());
                
                log.debug("  - Message ID: {} From: {} ({}) To: {} ({}) Content: {}", 
                    msg.getId(), senderEmail, senderId, recipientEmail, recipientId, 
                    msg.getMessageContent().substring(0, Math.min(30, msg.getMessageContent().length())));
                
                responseMsgs.add(msgMap);
            }
            
            log.info("✅ [DEBUG] Returning {} mapped messages to frontend", responseMsgs.size());
            return ResponseEntity.ok(responseMsgs);
        } catch (Exception e) {
            log.error("❌ Error fetching messages: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Admin sends message to a student
     * POST /admin/api/messages/send-to-student/{studentEmail}
     */
    @PostMapping("/api/messages/send-to-student/{studentEmail}")
    @PreAuthorize("hasAnyRole('ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> sendMessageToStudent(@PathVariable String studentEmail,
                                                   @RequestBody SendMessageRequest request) {
        try {
            String adminEmail = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User admin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            User student = userRepository.findByEmail(studentEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + studentEmail));
            
            // Validate message content
            if (request.getMessageContent() == null || request.getMessageContent().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Pesan tidak boleh kosong");
                        }});
            }
            
            if (request.getMessageContent().trim().length() < 5) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, Object>() {{
                            put("success", false);
                            put("message", "Pesan minimal 5 karakter");
                        }});
            }
            
            // Create and save message
            AdminMessage message = AdminMessage.builder()
                    .sender(admin)
                    .recipient(student)
                    .messageContent(request.getMessageContent().trim())
                    .messageType(request.getMessageType() != null ? request.getMessageType() : "ANSWER")
                    .status(AdminMessage.MessageStatus.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            adminMessageRepository.save(message);
            
            log.info("✅ Message sent from admin {} to student {}", adminEmail, studentEmail);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Pesan berhasil dikirim ke " + studentEmail);
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
     * Mark all unread messages as read for current admin
     * POST /admin/api/messages/mark-all-read
     */
    @PostMapping("/api/messages/mark-all-read")
    @PreAuthorize("hasAnyRole('ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> markAllMessagesAsRead() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentAdmin = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Get all unread messages where current admin is recipient
            List<AdminMessage> unreadMessages = adminMessageRepository.findAll().stream()
                    .filter(msg -> msg.getRecipient().getId().equals(currentAdmin.getId()) &&
                                   msg.getStatus() == AdminMessage.MessageStatus.UNREAD)
                    .collect(Collectors.toList());

            // Mark all as read
            unreadMessages.forEach(msg -> {
                msg.setStatus(AdminMessage.MessageStatus.READ);
                msg.setReadAt(LocalDateTime.now());
                adminMessageRepository.save(msg);
            });

            log.info("✅ Marked {} messages as read for admin {}", unreadMessages.size(), auth.getName());

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Semua pesan telah ditandai sebagai dibaca");
                put("markedCount", unreadMessages.size());
            }});
        } catch (Exception e) {
            log.error("❌ Error marking messages as read: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== EXPORT ==========

    /**
     * Export data formulir & pembayaran
     */
    @GetMapping("/api/export/formulir-pembayaran")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> exportFormAndPayment() {
        try {
            List<FormValidation> validations = formValidationRepository.findAll();
            
            List<Map<String, Object>> data = validations.stream().map(fv -> {
                Map<String, Object> row = new HashMap<>();
                row.put("Nama", fv.getStudent().getFullName());
                row.put("Email", fv.getStudent().getUser().getEmail());
                row.put("Status Form", fv.getValidationStatus().toString());
                row.put("Status Pembayaran", fv.getPaymentStatus().toString());
                row.put("Nomor VA", fv.getVirtualAccountNumber());
                row.put("Jumlah", fv.getPaymentAmount());
                row.put("Tanggal Dibuat", fv.getCreatedAt());
                row.put("Tanggal Divalidasi", fv.getValidatedAt());
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error exporting form and payment data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Export data daftar ulang
     */
    @GetMapping("/api/export/daftar-ulang")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> exportReEnrollmentData() {
        try {
            List<ReEnrollmentValidation> validations = reEnrollmentValidationRepository.findAll();
            
            List<Map<String, Object>> data = validations.stream().map(rv -> {
                Map<String, Object> row = new HashMap<>();
                row.put("Nama", rv.getStudent().getFullName());
                row.put("Email", rv.getStudent().getUser().getEmail());
                row.put("Status Validasi", rv.getValidationStatus().toString());
                row.put("Tanggal Dibuat", rv.getCreatedAt());
                row.put("Tanggal Divalidasi", rv.getValidatedAt());
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error exporting reenrollment data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Export data hasil akhir (dengan BRIVA)
     */
    @GetMapping("/api/export/hasil-akhir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> exportHasilAkhirData() {
        try {
            List<HasilAkhir> hasilAkhirList = hasilAkhirRepository.findAll();
            
            List<Map<String, Object>> data = hasilAkhirList.stream().map(ha -> {
                Map<String, Object> row = new HashMap<>();
                String studentName = ha.getStudent() != null ? ha.getStudent().getFullName() : "N/A";
                String studentEmail = ha.getUser() != null ? ha.getUser().getEmail() : "N/A";
                String nomorRegistrasi = ha.getNomorRegistrasi() != null ? ha.getNomorRegistrasi() : "-";
                
                row.put("Nama", studentName);
                row.put("Email", studentEmail);
                row.put("Nomor Registrasi", nomorRegistrasi);
                row.put("BRIVA Number", ha.getBrivaNumber() != null ? ha.getBrivaNumber() : "-");
                row.put("BRIVA Amount", ha.getBrivaAmount() != null ? ha.getBrivaAmount() : 0);
                row.put("Status", ha.getStatus() != null ? ha.getStatus().toString() : "PENDING");
                row.put("Tanggal Dibuat", ha.getCreatedAt());
                row.put("Tanggal Diupdate", ha.getUpdatedAt());
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error exporting hasil akhir data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== TABLE DATA EXPORT (For page rendering) ==========

    /**
     * Get admission forms table data with photos
     * Used for: Page 1 - Full table of admission forms
     */
    @GetMapping("/api/table/admission-forms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAdmissionFormsTable() {
        try {
            List<AdmissionForm> forms = admissionFormRepository.findAll();
            
            List<Map<String, Object>> data = forms.stream().map(form -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", form.getId());
                row.put("studentName", form.getFullName() != null ? form.getFullName() : "N/A");
                row.put("nik", form.getNik() != null ? form.getNik() : "N/A");
                row.put("email", form.getEmail() != null ? form.getEmail() : "N/A");
                row.put("phoneNumber", form.getPhoneNumber() != null ? form.getPhoneNumber() : "N/A");
                row.put("birthPlace", form.getBirthPlace() != null ? form.getBirthPlace() : "N/A");
                row.put("birthDate", form.getBirthDate() != null ? form.getBirthDate() : "N/A");
                row.put("gender", form.getGender() != null ? form.getGender() : "N/A");
                row.put("religion", form.getReligion() != null ? form.getReligion() : "N/A");
                row.put("city", form.getCity() != null ? form.getCity() : "N/A");
                row.put("province", form.getProvince() != null ? form.getProvince() : "N/A");
                row.put("schoolOrigin", form.getSchoolOrigin() != null ? form.getSchoolOrigin() : "N/A");
                row.put("schoolMajor", form.getSchoolMajor() != null ? form.getSchoolMajor() : "N/A");
                row.put("programStudi1", form.getProgramStudi1() != null ? form.getProgramStudi1() : "N/A");
                row.put("programStudi2", form.getProgramStudi2() != null ? form.getProgramStudi2() : "N/A");
                row.put("programStudi3", form.getProgramStudi3() != null ? form.getProgramStudi3() : "N/A");
                row.put("photoIdPath", form.getPhotoIdPath() != null ? form.getPhotoIdPath() : "");
                row.put("createdAt", form.getCreatedAt());
                row.put("updatedAt", form.getUpdatedAt());
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", data.size());
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching admission forms table: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get reenrollments table data with documents
     * Used for: Page 2 - Full table of reenrollments
     */
    @GetMapping("/api/table/reenrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getReenrollmentsTable() {
        try {
            List<ReEnrollment> reenrollments = reenrollmentRepository.findAll();
            
            List<Map<String, Object>> data = reenrollments.stream().map(re -> {
                Map<String, Object> row = new HashMap<>();
                Student student = re.getStudent();
                row.put("id", re.getId());
                row.put("studentName", student != null ? student.getFullName() : "N/A");
                row.put("nik", student != null ? student.getNik() : "N/A");
                row.put("email", student != null ? (student.getUser() != null ? student.getUser().getEmail() : "N/A") : "N/A");
                row.put("status", re.getStatus() != null ? re.getStatus().toString() : "PENDING");
                row.put("validationNotes", re.getValidationNotes() != null ? re.getValidationNotes() : "");
                row.put("submittedAt", re.getSubmittedAt());
                row.put("validatedAt", re.getValidatedAt());
                row.put("createdAt", re.getCreatedAt());
                row.put("updatedAt", re.getUpdatedAt());
                
                // Get documents list
                List<Map<String, String>> documents = new ArrayList<>();
                try {
                    List<ReEnrollmentDocument> docs = reenrollmentDocumentRepository.findByReenrollmentId(re.getId());
                    if (docs != null) {
                        docs.forEach(doc -> {
                            Map<String, String> docMap = new HashMap<>();
                            docMap.put("type", doc.getDocumentType() != null ? doc.getDocumentType().toString() : "");
                            docMap.put("filename", doc.getOriginalFilename() != null ? doc.getOriginalFilename() : "");
                            docMap.put("path", doc.getFilePath() != null ? doc.getFilePath() : "");
                            documents.add(docMap);
                        });
                    }
                } catch (Exception e) {
                    log.debug("Warning: Could not fetch documents for reenrollment {}: {}", re.getId(), e.getMessage());
                }
                row.put("documents", documents);
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", data.size());
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching reenrollments table: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get hasil akhir table with wave type, selection type, program studi
     * Used for: Page 3 - Full hasil akhir detail table
     */
    @GetMapping("/api/table/hasil-akhir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getHasilAkhirTable() {
        try {
            List<HasilAkhir> hasilAkhirList = hasilAkhirRepository.findAll();
            
            List<Map<String, Object>> data = hasilAkhirList.stream().map(ha -> {
                Map<String, Object> row = new HashMap<>();
                Student student = ha.getStudent();
                row.put("id", ha.getId());
                row.put("studentName", student != null ? student.getFullName() : "N/A");
                row.put("nik", student != null ? student.getNik() : "N/A");
                row.put("email", student != null ? (student.getUser() != null ? student.getUser().getEmail() : "N/A") : "N/A");
                row.put("nomorRegistrasi", ha.getNomorRegistrasi() != null ? ha.getNomorRegistrasi() : "-");
                row.put("brivaNumber", ha.getBrivaNumber() != null ? ha.getBrivaNumber() : "-");
                row.put("brivaAmount", ha.getBrivaAmount() != null ? ha.getBrivaAmount().toString() : "0");
                row.put("jumlahCicilan", ha.getJumlahCicilan() != null ? ha.getJumlahCicilan() : 1);
                row.put("waveType", ha.getWaveType() != null ? ha.getWaveType().toString() : "N/A");
                row.put("selectionType", ha.getSelectionType() != null ? ha.getSelectionType() : "N/A");
                row.put("programStudiName", ha.getProgramStudiName() != null ? ha.getProgramStudiName() : "N/A");
                row.put("status", ha.getStatus() != null ? ha.getStatus().toString() : "PENDING");
                row.put("createdAt", ha.getCreatedAt());
                row.put("updatedAt", ha.getUpdatedAt());
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", data.size());
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching hasil akhir table: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get hasil akhir filtered by wave type
     * Used for: Page 4 - Download by gelombang (wave type)
     * Example: GET /admin/api/table/hasil-akhir/by-wave/REGULAR_TEST
     */
    @GetMapping("/api/table/hasil-akhir/by-wave/{waveType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getHasilAkhirByWave(
            @PathVariable String waveType) {
        try {
            // Parse wave type
            RegistrationPeriod.WaveType wave;
            try {
                wave = RegistrationPeriod.WaveType.valueOf(waveType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid wave type: " + waveType));
            }
            
            // Filter hasil akhir by wave type
            List<HasilAkhir> hasilAkhirList = hasilAkhirRepository.findAll().stream()
                    .filter(ha -> ha.getWaveType() != null && ha.getWaveType().equals(wave))
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> data = hasilAkhirList.stream().map(ha -> {
                Map<String, Object> row = new HashMap<>();
                Student student = ha.getStudent();
                row.put("id", ha.getId());
                row.put("studentName", student != null ? student.getFullName() : "N/A");
                row.put("nik", student != null ? student.getNik() : "N/A");
                row.put("email", student != null ? (student.getUser() != null ? student.getUser().getEmail() : "N/A") : "N/A");
                row.put("nomorRegistrasi", ha.getNomorRegistrasi() != null ? ha.getNomorRegistrasi() : "-");
                row.put("brivaNumber", ha.getBrivaNumber() != null ? ha.getBrivaNumber() : "-");
                row.put("brivaAmount", ha.getBrivaAmount() != null ? ha.getBrivaAmount().toString() : "0");
                row.put("jumlahCicilan", ha.getJumlahCicilan() != null ? ha.getJumlahCicilan() : 1);
                row.put("waveType", ha.getWaveType() != null ? ha.getWaveType().toString() : "N/A");
                row.put("selectionType", ha.getSelectionType() != null ? ha.getSelectionType() : "N/A");
                row.put("programStudiName", ha.getProgramStudiName() != null ? ha.getProgramStudiName() : "N/A");
                row.put("status", ha.getStatus() != null ? ha.getStatus().toString() : "PENDING");
                row.put("createdAt", ha.getCreatedAt());
                row.put("updatedAt", ha.getUpdatedAt());
                return row;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("waveType", waveType);
            response.put("totalRecords", data.size());
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching hasil akhir by wave: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }


    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class RegistrationPeriodRequest {
        private String name;
        private LocalDateTime regStartDate;
        private LocalDateTime regEndDate;
        private LocalDateTime examDate;
        private LocalDateTime examEndDate;
        private LocalDateTime announcementDate;
        private LocalDateTime reenrollmentStartDate;
        private LocalDateTime reenrollmentEndDate;
        private String description;
        private String requirements; // e.g., "Wajib upload nilai, bukti ranking, dll"
        
        // ✅ NEW: Wave type - determines registration flow
        private RegistrationPeriod.WaveType waveType;
        
        // ✅ NEW: List of Jenis Seleksi IDs for this period
        private List<Long> jenisSeleksiIds;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SelectionTypeRequest {
        private Long periodId;
        private String name;
        private String description;
        private Boolean requireRanking;
        private Boolean requireTesting;
        private SelectionType.FormType formType;
        private BigDecimal price;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ValidationRequest {
        private Boolean approved;
        private String reason;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApiResponse {
        private Boolean success;
        private String message;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class FormValidationRejectRequest {
        private String topic;
        private String reason;
        private Integer revisionNumber;  // Which revision number (1, 2, 3, etc)
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SendMessageRequest {
        private Long recipientId;
        private String messageContent;
        private String messageType;
        private Long admissionFormId;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class HasilAkhirRegistrationRequest {
        private String nomorRegistrasi;
        private String brivaNumber;
        private Integer jumlahCicilan;
    }

    // ========== ANNOUNCEMENTS MANAGEMENT SYSTEM ==========

    /**
     * Create new announcement - Admin Pusat & Validasi
     */
    @PostMapping("/api/announcements")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> createAnnouncement(@Valid @RequestBody CreateAnnouncementRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User adminUser = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            Announcement announcement = Announcement.builder()
                    .title(request.getTitle())
                    .content(request.getContent())
                    .description(request.getDescription())
                    .createdByName(adminUser.getEmail())
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .priority(request.getPriority() != null ? request.getPriority() : 0)
                    .announcementType(Announcement.AnnouncementType.valueOf(
                        request.getAnnouncementType() != null ? request.getAnnouncementType() : "GENERAL"))
                    .build();

            Announcement saved = announcementRepository.save(announcement);
            log.info("✅ Announcement created by {}: {}", adminUser.getEmail(), saved.getTitle());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Pengumuman berhasil dibuat"));
        } catch (Exception e) {
            log.error("❌ Error creating announcement: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Get all active announcements for camaba
     */
    @GetMapping("/api/announcements")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getAllAnnouncements() {
        try {
            List<Announcement> announcements = announcementRepository.findAllActive();
            List<AnnouncementDTO> dtos = announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtos);
            response.put("count", dtos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching announcements: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Get announcements with pagination
     */
    @GetMapping("/api/announcements/paginated")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getAnnouncementsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            org.springframework.data.domain.Page<Announcement> result = 
                announcementRepository.findAllActive(org.springframework.data.domain.PageRequest.of(page, size));

            List<AnnouncementDTO> dtos = result.getContent().stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtos);
            response.put("totalPages", result.getTotalPages());
            response.put("totalElements", result.getTotalElements());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching announcements: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Get single announcement by ID
     */
    @GetMapping("/api/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getAnnouncementById(@PathVariable Long id) {
        try {
            Announcement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pengumuman tidak ditemukan"));

            if (!announcement.getIsActive()) {
                throw new RuntimeException("Pengumuman tidak aktif");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", AnnouncementDTO.fromEntity(announcement));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching announcement: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Get recent announcements (last 5)
     */
    @GetMapping("/api/announcements/recent")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getRecentAnnouncements() {
        try {
            List<Announcement> announcements = announcementRepository.findRecent();
            List<AnnouncementDTO> dtos = announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtos);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching recent announcements: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Get urgent announcements
     */
    @GetMapping("/api/announcements/urgent")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> getUrgentAnnouncements() {
        try {
            List<Announcement> announcements = announcementRepository.findUrgent();
            List<AnnouncementDTO> dtos = announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtos);
            response.put("count", dtos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching urgent announcements: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Update announcement - Admin Pusat & Validasi
     */
    @PutMapping("/api/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        try {
            Announcement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pengumuman tidak ditemukan"));

            announcement.setTitle(request.getTitle());
            announcement.setContent(request.getContent());
            announcement.setDescription(request.getDescription());
            announcement.setPriority(request.getPriority() != null ? request.getPriority() : 0);
            announcement.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            announcement.setAnnouncementType(Announcement.AnnouncementType.valueOf(
                request.getAnnouncementType() != null ? request.getAnnouncementType() : "GENERAL"));

            announcementRepository.save(announcement);
            log.info("✅ Announcement updated: {}", announcement.getTitle());

            return ResponseEntity.ok(new ApiResponse(true, "Pengumuman berhasil diperbarui"));
        } catch (Exception e) {
            log.error("❌ Error updating announcement: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Delete announcement - Admin Pusat & Validasi
     */
    @DeleteMapping("/api/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        try {
            Announcement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pengumuman tidak ditemukan"));

            announcementRepository.delete(announcement);
            log.info("✅ Announcement deleted: {}", announcement.getTitle());

            return ResponseEntity.ok(new ApiResponse(true, "Pengumuman berhasil dihapus"));
        } catch (Exception e) {
            log.error("❌ Error deleting announcement: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Deactivate/Disable announcement (soft delete)
     */
    @PutMapping("/api/announcements/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> deactivateAnnouncement(@PathVariable Long id) {
        try {
            Announcement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pengumuman tidak ditemukan"));

            announcement.setIsActive(false);
            announcementRepository.save(announcement);
            log.info("✅ Announcement deactivated: {}", announcement.getTitle());

            return ResponseEntity.ok(new ApiResponse(true, "Pengumuman berhasil dinonaktifkan"));
        } catch (Exception e) {
            log.error("❌ Error deactivating announcement: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Search announcements by keyword
     */
    @GetMapping("/api/announcements/search/{keyword}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI', 'CAMABA')")
    public ResponseEntity<?> searchAnnouncements(@PathVariable String keyword) {
        try {
            List<Announcement> announcements = announcementRepository.searchByTitle(keyword);
            List<AnnouncementDTO> dtos = announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtos);
            response.put("count", dtos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error searching announcements: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Get all users - Admin only (any admin role)
     */
    @GetMapping("/api/users")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            List<Map<String, Object>> userDtos = users.stream()
                    .map(user -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", user.getId());
                        dto.put("email", user.getEmail());
                        dto.put("fullName", user.getEmail()); // Gunakan email sebagai nama
                        dto.put("role", user.getRole().toString());
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userDtos);
            response.put("total", userDtos.size());

            log.info("✅ Retrieved {} users", userDtos.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching users: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Update user role - Admin Pusat only
     */
    @PutMapping("/api/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            String roleStr = request.get("role");
            if (roleStr == null || roleStr.isEmpty()) {
                throw new RuntimeException("Role tidak valid");
            }

            User.UserRole newRole = User.UserRole.valueOf(roleStr);
            user.setRole(newRole);
            userRepository.save(user);

            log.info("✅ User role updated: {} -> {}", user.getEmail(), newRole);
            return ResponseEntity.ok(new ApiResponse(true, "Role berhasil diubah"));
        } catch (Exception e) {
            log.error("❌ Error updating user role: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Delete user - Admin Pusat only (cannot delete current user)
     */
    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentEmail = auth.getName();
            
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            // Prevent deleting self
            if (user.getEmail().equals(currentEmail)) {
                throw new RuntimeException("Tidak bisa menghapus akun Anda sendiri");
            }

            userRepository.delete(user);
            log.info("✅ User deleted: {}", user.getEmail());

            return ResponseEntity.ok(new ApiResponse(true, "Akun berhasil dihapus"));
        } catch (Exception e) {
            log.error("❌ Error deleting user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Update registration period (Admin Pusat only)
     */
    @PutMapping("/periods/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    @Transactional
    public ResponseEntity<?> updateRegistrationPeriod(@PathVariable Long id, 
                                                      @Valid @RequestBody RegistrationPeriodRequest request) {
        try {
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("🔥🔥🔥 [CONTROLLER HIT] PUT /admin/periods/{id}");
            System.out.println("║ ID: " + id);
            
            // 🔍 DEBUG: Log method entry with security context
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            
            System.out.println("║ AUTH OBJECT: " + SecurityContextHolder.getContext().getAuthentication());
            System.out.println("║ USER: " + email);
            System.out.println("║ AUTHORITIES: " + authorities.stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toList()));
            System.out.println("╚════════════════════════════════════════════╝");
            
            RegistrationPeriod period = registrationPeriodRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Periode tidak ditemukan"));

            period.setName(request.getName());
            period.setRegStartDate(request.getRegStartDate());
            period.setRegEndDate(request.getRegEndDate());
            period.setExamDate(request.getExamDate());
            period.setExamEndDate(request.getExamEndDate());
            period.setAnnouncementDate(request.getAnnouncementDate());
            period.setReenrollmentStartDate(request.getReenrollmentStartDate());
            period.setReenrollmentEndDate(request.getReenrollmentEndDate());
            period.setDescription(request.getDescription());
            period.setRequirements(request.getRequirements());
            
            // ✅ NEW: Update wave type
            if (request.getWaveType() != null) {
                period.setWaveType(request.getWaveType());
            }

            registrationPeriodRepository.save(period);
            
            // ✅ FIXED: Update jenis seleksi relationships
            // First, delete existing relationships and flush immediately
            periodJenisSeleksiRepository.deleteByPeriod_Id(id);
            entityManager.flush(); // ✅ FIX: Flush the delete to database before inserting new records
            
            // Then, create new ones
            if (request.getJenisSeleksiIds() != null && !request.getJenisSeleksiIds().isEmpty()) {
                for (Long jenisSeleksiId : request.getJenisSeleksiIds()) {
                    JenisSeleksi jenisSeleksi = jenisSeleksiRepository.findById(jenisSeleksiId).orElse(null);
                    if (jenisSeleksi != null) {
                        PeriodJenisSeleksi pjs = PeriodJenisSeleksi.builder()
                                .period(period)
                                .jenisSeleksi(jenisSeleksi)
                                .isActive(true)
                                .build();
                        periodJenisSeleksiRepository.save(pjs);
                    }
                }
                log.info("✅ Updated {} jenis seleksi for period: {}", request.getJenisSeleksiIds().size(), period.getName());
            }
            
            log.info("✅ Period updated: ID={}, Name={}", id, period.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Periode berhasil diperbarui"));
        } catch (Exception e) {
            log.error("❌ Error updating period: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Delete registration period (Admin Pusat only)
     */
    @DeleteMapping("/periods/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    @Transactional
    public ResponseEntity<?> deleteRegistrationPeriod(@PathVariable Long id) {
        try {
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("🔥🔥🔥 [CONTROLLER HIT] DELETE /admin/periods/{id}");
            System.out.println("║ ID: " + id);
            
            // 🔍 DEBUG: Log method entry
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
            
            System.out.println("║ AUTH OBJECT: " + SecurityContextHolder.getContext().getAuthentication());
            System.out.println("║ USER: " + email);
            System.out.println("║ AUTHORITIES: " + authorities.stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toList()));
            System.out.println("╚════════════════════════════════════════════╝");
            
            RegistrationPeriod period = registrationPeriodRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Periode tidak ditemukan"));

            // ✅ FIXED: Delete child records (PERIOD_JENIS_SELEKSI) first before deleting parent
            periodJenisSeleksiRepository.deleteByPeriod_Id(id);
            entityManager.flush(); // ✅ FIX: Flush to ensure children are deleted before parent
            
            registrationPeriodRepository.delete(period);
            log.info("✅ Period deleted: ID={}, Name={}", id, period.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Periode berhasil dihapus"));
        } catch (Exception e) {
            log.error("❌ Error deleting period: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get exam Google Form link
     */
    @GetMapping("/api/gform-link")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getExamGFormLink() {
        try {
            SystemConfiguration config = systemConfigRepository
                    .findByConfigKey("exam_gform_link")
                    .orElse(null);

            if (config == null || !config.getIsActive()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "gformLink", "",
                    "message", "Belum ada link Google Form yang diset"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "gformLink", config.getConfigValue()
            ));
        } catch (Exception e) {
            log.error("❌ Error getting gform link: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Set exam Google Form link (Admin Pusat only)
     * Only 1 link allowed at a time
     */
    @PostMapping("/api/gform-link")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> setExamGFormLink(@RequestBody Map<String, String> request) {
        try {
            String gformLink = request.get("gformLink");
            
            if (gformLink == null || gformLink.trim().isEmpty()) {
                throw new RuntimeException("Link Google Form tidak boleh kosong");
            }

            // Check if link already exists
            SystemConfiguration existing = systemConfigRepository
                    .findByConfigKey("exam_gform_link")
                    .orElse(null);

            if (existing != null && !existing.getConfigValue().equals(gformLink)) {
                // Link sudah ada dan berbeda, update saja
                existing.setConfigValue(gformLink);
                existing.setUpdatedAt(LocalDateTime.now());
                systemConfigRepository.save(existing);
                log.info("✅ GForm link updated");
            } else if (existing == null) {
                // Link belum ada, buat baru
                SystemConfiguration config = SystemConfiguration.builder()
                        .configKey("exam_gform_link")
                        .configValue(gformLink)
                        .description("URL Google Form untuk ujian online")
                        .configType(SystemConfiguration.ConfigType.STRING)
                        .isActive(true)
                        .build();
                systemConfigRepository.save(config);
                log.info("✅ GForm link created");
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Link Google Form berhasil disimpan",
                "gformLink", gformLink
            ));
        } catch (Exception e) {
            log.error("❌ Error setting gform link: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Delete exam Google Form link (Admin Pusat only)
     */
    @DeleteMapping("/api/gform-link")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteExamGFormLink() {
        try {
            SystemConfiguration config = systemConfigRepository
                    .findByConfigKey("exam_gform_link")
                    .orElse(null);

            if (config == null) {
                throw new RuntimeException("Link Google Form tidak ditemukan");
            }

            systemConfigRepository.delete(config);
            log.info("✅ GForm link deleted");
            return ResponseEntity.ok(new ApiResponse(true, "Link Google Form berhasil dihapus"));
        } catch (Exception e) {
            log.error("❌ Error deleting gform link: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all system settings (public - anyone can read)
     */
    @GetMapping("/api/settings")
    public ResponseEntity<?> getSystemSettings() {
        try {
            List<SystemConfiguration> settings = systemConfigRepository.findByIsActive(true);
            
            Map<String, String> settingsMap = new HashMap<>();
            settings.forEach(s -> settingsMap.put(s.getConfigKey(), s.getConfigValue()));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", settingsMap
            ));
        } catch (Exception e) {
            log.error("❌ Error getting settings: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get single setting by key (public)
     */
    @GetMapping("/api/settings/{key}")
    public ResponseEntity<?> getSetting(@PathVariable String key) {
        try {
            SystemConfiguration setting = systemConfigRepository
                    .findByConfigKey(key)
                    .filter(SystemConfiguration::getIsActive)
                    .orElse(null);

            if (setting == null) {
                return ResponseEntity.ok(Map.of("value", ""));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "key", setting.getConfigKey(),
                "value", setting.getConfigValue()
            ));
        } catch (Exception e) {
            log.error("❌ Error getting setting: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("value", ""));
        }
    }

    /**
     * Update system setting (Admin Pusat only)
     */
    @PutMapping("/api/settings/{key}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateSetting(@PathVariable String key, 
                                          @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            if (value == null) {
                throw new RuntimeException("Value tidak boleh kosong");
            }

            SystemConfiguration setting = systemConfigRepository
                    .findByConfigKey(key)
                    .orElse(null);

            if (setting == null) {
                // Create new setting
                setting = SystemConfiguration.builder()
                        .configKey(key)
                        .configValue(value)
                        .configType(SystemConfiguration.ConfigType.STRING)
                        .isActive(true)
                        .build();
                log.info("✅ New setting created: {}", key);
            } else {
                // Update existing
                setting.setConfigValue(value);
                setting.setUpdatedAt(LocalDateTime.now());
                log.info("✅ Setting updated: {} = {}", key, value);
            }

            systemConfigRepository.save(setting);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Setting berhasil disimpan",
                "key", key,
                "value", value
            ));
        } catch (Exception e) {
            log.error("❌ Error updating setting: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== EXAM LINK MANAGEMENT ==========

    /**
     * Create exam link (Admin Pusat only)
     */
    @PostMapping("/api/exam-links")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createExamLink(@Valid @RequestBody CreateExamLinkRequest request) {
        try {
            RegistrationPeriod period = registrationPeriodRepository.findById(request.getPeriodId())
                    .orElseThrow(() -> new RuntimeException("Period tidak ditemukan"));

            SelectionType selectionType = null;
            if (request.getSelectionTypeId() != null) {
                selectionType = selectionTypeRepository.findById(request.getSelectionTypeId())
                        .orElse(null);
            }

            // Validate link URL
            if (!request.getLinkUrl().contains("forms.google.com") && !request.getLinkUrl().contains("forms.gle")) {
                throw new RuntimeException("Link harus menggunakan Google Form (forms.google.com atau forms.gle)");
            }

            ExamLink examLink = ExamLink.builder()
                    .period(period)
                    .selectionType(selectionType)
                    .linkTitle(request.getLinkTitle())
                    .linkUrl(request.getLinkUrl())
                    .description(request.getDescription())
                    .isActive(true)
                    .build();

            examLinkRepository.save(examLink);
            log.info("✅ Exam link created: {} for period {}", request.getLinkTitle(), period.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Link ujian berhasil dibuat"));
        } catch (Exception e) {
            log.error("❌ Error creating exam link: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get exam links by period
     */
    @GetMapping("/api/exam-links/period/{periodId}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getExamLinksByPeriod(@PathVariable Long periodId) {
        try {
            List<ExamLink> links = examLinkRepository.findByPeriodId(periodId);
            
            List<Map<String, Object>> linkDtos = links.stream()
                    .map(link -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", link.getId());
                        dto.put("periodId", link.getPeriod().getId());
                        dto.put("periodName", link.getPeriod().getName());
                        dto.put("selectionTypeId", link.getSelectionType() != null ? link.getSelectionType().getId() : null);
                        dto.put("selectionTypeName", link.getSelectionType() != null ? link.getSelectionType().getName() : "General");
                        dto.put("linkTitle", link.getLinkTitle());
                        dto.put("linkUrl", link.getLinkUrl());
                        dto.put("description", link.getDescription());
                        dto.put("isActive", link.getIsActive());
                        dto.put("createdAt", link.getCreatedAt());
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", linkDtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching exam links: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Update exam link (Admin Pusat only)
     */
    @PutMapping("/api/exam-links/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateExamLink(@PathVariable Long id, 
                                           @Valid @RequestBody CreateExamLinkRequest request) {
        try {
            ExamLink link = examLinkRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Link ujian tidak ditemukan"));

            if (!request.getLinkUrl().contains("forms.google.com") && !request.getLinkUrl().contains("forms.gle")) {
                throw new RuntimeException("Link harus menggunakan Google Form");
            }

            link.setLinkTitle(request.getLinkTitle());
            link.setLinkUrl(request.getLinkUrl());
            link.setDescription(request.getDescription());
            link.setUpdatedAt(LocalDateTime.now());

            examLinkRepository.save(link);
            log.info("✅ Exam link updated: {}", request.getLinkTitle());

            return ResponseEntity.ok(new ApiResponse(true, "Link ujian berhasil diperbarui"));
        } catch (Exception e) {
            log.error("❌ Error updating exam link: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Delete exam link (Admin Pusat only)
     */
    @DeleteMapping("/api/exam-links/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteExamLink(@PathVariable Long id) {
        try {
            ExamLink link = examLinkRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Link ujian tidak ditemukan"));

            examLinkRepository.delete(link);
            log.info("✅ Exam link deleted: {}", link.getLinkTitle());

            return ResponseEntity.ok(new ApiResponse(true, "Link ujian berhasil dihapus"));
        } catch (Exception e) {
            log.error("❌ Error deleting exam link: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get all selection types by period (Admin Pusat only)
     * Returns structured data for frontend
     */
    @GetMapping("/api/selection-types/period/{periodId}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getSelectionTypesByPeriod(@PathVariable Long periodId) {
        try {
            RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new RuntimeException("Period tidak ditemukan"));

            List<SelectionType> selectionTypes = selectionTypeRepository.findByPeriod_Id(periodId);

            List<Map<String, Object>> typeDtos = selectionTypes.stream()
                    .map(type -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", type.getId());
                        dto.put("name", type.getName());
                        dto.put("description", type.getDescription());
                        dto.put("formType", type.getFormType().toString());
                        dto.put("requireRanking", type.getRequireRanking());
                        dto.put("requireTesting", type.getRequireTesting());
                        dto.put("price", type.getPrice());
                        dto.put("isActive", type.getIsActive());
                        dto.put("createdAt", type.getCreatedAt());
                        return dto;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("periodId", period.getId());
            response.put("periodName", period.getName());
            response.put("data", typeDtos);
            response.put("total", typeDtos.size());

            log.info("✅ Retrieved {} selection types for period {}", typeDtos.size(), period.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching selection types: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Update selection type (Admin Pusat only)
     */
    @PutMapping("/api/selection-types/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> updateSelectionType(@PathVariable Long id,
                                                @Valid @RequestBody UpdateSelectionTypeRequest request) {
        try {
            SelectionType type = selectionTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Jenis seleksi tidak ditemukan"));

            type.setName(request.getName());
            type.setDescription(request.getDescription());
            type.setRequireRanking(request.getRequireRanking());
            type.setRequireTesting(request.getRequireTesting());
            type.setPrice(request.getPrice());
            type.setIsActive(request.getIsActive());
            type.setUpdatedAt(LocalDateTime.now());

            selectionTypeRepository.save(type);
            log.info("✅ Selection type updated: {}", request.getName());

            return ResponseEntity.ok(new ApiResponse(true, "Jenis seleksi berhasil diperbarui"));
        } catch (Exception e) {
            log.error("❌ Error updating selection type: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Delete selection type (Admin Pusat only)
     */
    @DeleteMapping("/api/selection-types/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteSelectionType(@PathVariable Long id) {
        try {
            SelectionType type = selectionTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Jenis seleksi tidak ditemukan"));

            selectionTypeRepository.delete(type);
            log.info("✅ Selection type deleted: {}", type.getName());

            return ResponseEntity.ok(new ApiResponse(true, "Jenis seleksi berhasil dihapus"));
        } catch (Exception e) {
            log.error("❌ Error deleting selection type: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== DTOs FOR EXAM LINKS ==========

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CreateExamLinkRequest {
        private Long periodId;
        private Long selectionTypeId; // Optional: null for general exam link
        private String linkTitle;
        private String linkUrl;
        private String description;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class UpdateSelectionTypeRequest {
        private String name;
        private String description;
        private Boolean requireRanking;
        private Boolean requireTesting;
        private BigDecimal price;
        private Boolean isActive;
    }

    // ========== JENIS SELEKSI DTO ==========
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class JenisSeleksiRequest {
        private String code;          // e.g., "REGULAR", "SCHOLARSHIP"
        private String nama;          // e.g., "Seleksi Reguler"
        private String deskripsi;     // Description
        private String fasilitas;     // Features (comma-separated or JSON)
        private String logoUrl;       // Logo/Icon URL or emoji
        private BigDecimal harga;     // Registration fee
        private Boolean isActive;     // Active/Inactive
        private Integer sortOrder;    // Display order
        private List<Long> programStudiIds;  // M2M: Related program studi IDs
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ProgramStudiRequest {
        private String kode;          // e.g., "TI", "SI"
        private String nama;          // e.g., "Teknik Informatika"
        private String deskripsi;     // Description
        private Boolean isMedical;    // Whether it's a medical program
        private Boolean isActive;     // Active/Inactive
        private Integer sortOrder;    // Display order
        private Long hargaTotalPerTahun;  // Total program fee per year (for installments)
        private Long cicilan1;             // First installment amount
        private Long cicilan2;
        private Long cicilan3;
        private Long cicilan4;
        private Long cicilan5;
        private Long cicilan6;
    }

    // ========== PAYMENT & EXAM VERIFICATION ==========

    /**
     * Verify payment status for a form
     */
    @PostMapping("/api/validasi/formulir/{id}/verify-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> verifyPayment(@PathVariable Long id) {
        try {
            AdmissionForm form = admissionFormRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Form tidak ditemukan"));

            // Find validation record for this form
            List<FormValidation> validations = formValidationRepository.findAll().stream()
                    .filter(v -> v.getAdmissionForm().getId().equals(id))
                    .limit(1)
                    .toList();

            FormValidation validation;
            if (validations.isEmpty()) {
                // Create new validation if doesn't exist
                validation = new FormValidation();
                validation.setAdmissionForm(form);
                validation.setStudent(form.getStudent());
                validation.setCreatedAt(LocalDateTime.now());
                validation = formValidationRepository.save(validation);
                
                // ✅ NEW: Create corresponding FormRepairStatus record
                FormRepairStatus repairStatus = FormRepairStatus.builder()
                        .formValidation(validation)
                        .status(FormRepairStatus.RepairStatus.BELUM_PERBAIKAN)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                formRepairStatusRepository.save(repairStatus);
                log.info("✅ Created new FormRepairStatus with BELUM_PERBAIKAN for validation: {}", validation.getId());
            } else {
                validation = validations.get(0);
            }

            // Update payment status to VERIFIED
            validation.setPaymentStatus(FormValidation.PaymentStatus.VERIFIED);
            validation.setUpdatedAt(LocalDateTime.now());
            formValidationRepository.save(validation);

            log.info("✅ Payment verified for form {}", id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Pembayaran berhasil diverifikasi",
                "paymentStatus", validation.getPaymentStatus().toString()
            ));
        } catch (Exception e) {
            log.error("❌ Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Generate exam access token for a form
     */
    @PostMapping("/api/validasi/formulir/{id}/generate-exam-token")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_VALIDASI', 'ADMIN_PUSAT')")
    public ResponseEntity<?> generateExamToken(@PathVariable Long id) {
        try {
            AdmissionForm form = admissionFormRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Form tidak ditemukan"));

            // Find validation record for this form
            List<FormValidation> validations = formValidationRepository.findAll().stream()
                    .filter(v -> v.getAdmissionForm().getId().equals(id))
                    .limit(1)
                    .toList();

            FormValidation validation;
            if (validations.isEmpty()) {
                // Create new validation if doesn't exist
                validation = new FormValidation();
                validation.setAdmissionForm(form);
                validation.setStudent(form.getStudent());
                validation.setCreatedAt(LocalDateTime.now());
                validation = formValidationRepository.save(validation);
                
                // ✅ NEW: Create corresponding FormRepairStatus record
                FormRepairStatus repairStatus = FormRepairStatus.builder()
                        .formValidation(validation)
                        .status(FormRepairStatus.RepairStatus.BELUM_PERBAIKAN)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                formRepairStatusRepository.save(repairStatus);
                log.info("✅ Created new FormRepairStatus with BELUM_PERBAIKAN for validation: {}", validation.getId());
            } else {
                validation = validations.get(0);
            }

            // Generate exam token (simple format: formId + timestamp)
            String examToken = form.getId() + "-" + System.currentTimeMillis();
            validation.setExamToken(examToken);
            validation.setUpdatedAt(LocalDateTime.now());
            formValidationRepository.save(validation);

            log.info("✅ Exam token generated for form {}: {}", id, examToken);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Token ujian berhasil dibuat",
                "examToken", examToken,
                "formId", form.getId()
            ));
        } catch (Exception e) {
            log.error("❌ Error generating exam token: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Get exam token for student
     */
    @GetMapping("/validasi/formulir/{id}/exam-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getExamToken(@PathVariable Long id) {
        try {
            AdmissionForm form = admissionFormRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Form tidak ditemukan"));

            // Find validation record for this form
            List<FormValidation> validations = formValidationRepository.findAll().stream()
                    .filter(v -> v.getAdmissionForm().getId().equals(id))
                    .limit(1)
                    .toList();

            if (validations.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(false, "Token ujian belum tersedia"));
            }

            FormValidation validation = validations.get(0);
            String token = validation.getExamToken();

            if (token == null) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(false, "Token ujian belum tersedia"));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "examToken", token,
                "formId", form.getId()
            ));
        } catch (Exception e) {
            log.error("❌ Error getting exam token: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========== ADMIN REMINDERS ==========

    /**
     * ✅ NEW: Send reminder message to student (contextual based on reminder type)
     * Manual admin reminder endpoint
     */
    @PostMapping("/api/send-reminder")
    @PreAuthorize("hasAnyRole('ADMIN_VALIDASI', 'ADMIN_PUSAT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> sendReminderToStudent(@RequestBody SendReminderRequest request) {
        try {
            // ============ DEBUG AUTHORIZATION ============
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.info("🔐 [REMINDER] Authorization Debug:");
            log.info("   Principal: {}", auth != null ? auth.getPrincipal() : "null");
            log.info("   Authenticated: {}", auth != null ? auth.isAuthenticated() : "NO AUTH");
            log.info("   Authorities: {}", auth != null ? auth.getAuthorities() : "NONE");
            if (auth != null && auth.getPrincipal() instanceof UserDetails) {
                UserDetails ud = (UserDetails) auth.getPrincipal();
                log.info("   Username: {}", ud.getUsername());
                log.info("   Granted Authorities: {}", ud.getAuthorities());
            }
            // ================================================
            
            String studentEmail = request.getStudentEmail();
            String messageTitle = request.getMessageTitle();
            String messageBody = request.getMessageBody();
            String reminderType = request.getReminderType();

            log.info("📧 [ADMIN-REMINDER] Sending {} reminder to {}", reminderType, studentEmail);

            // Send email
            emailService.sendHtmlEmail(
                studentEmail,
                messageTitle,
                messageBody
            );

            log.info("✅ [ADMIN-REMINDER-SENT] Email sent successfully to {}", studentEmail);

            return ResponseEntity.ok(new ApiResponse(true, "Reminder berhasil dikirim ke " + studentEmail));
        } catch (Exception e) {
            log.error("❌ Error sending reminder: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Gagal mengirim reminder: " + e.getMessage()));
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SendReminderRequest {
        private String studentEmail;
        private String studentName;
        private String messageTitle;
        private String messageBody;
        private String reminderType; // PENDING, NOT_PAID, APPROVED, INCOMPLETE
        private Long formulirId;
    }

    /**
     * ✅ NEW: Get all exam submissions pending validation
     * GET /admin/exam-submissions
     */
    @GetMapping("/exam-submissions")
    @PreAuthorize("hasRole('ADMIN_VALIDASI') or hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> getExamSubmissions(
            @RequestParam(defaultValue = "PENDING") String status) {
        try {
            List<ExamResult> results;
            
            if ("PENDING".equals(status)) {
                results = examResultRepository.findByExamValidationStatus(ExamResult.ExamValidationStatus.PENDING);
            } else if ("APPROVED".equals(status)) {
                results = examResultRepository.findByExamValidationStatus(ExamResult.ExamValidationStatus.APPROVED);
            } else if ("REJECTED".equals(status)) {
                results = examResultRepository.findByExamValidationStatus(ExamResult.ExamValidationStatus.REJECTED);
            } else if ("REVISI".equals(status)) {
                results = examResultRepository.findByExamValidationStatus(ExamResult.ExamValidationStatus.REVISI);
            } else {
                results = examResultRepository.findAll();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (ExamResult result : results) {
                if (result.getExam() != null && result.getExam().getStudent() != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", result.getId());
                    map.put("studentId", result.getExam().getStudent().getId());
                    map.put("studentName", result.getExam().getStudent().getFullName());
                    map.put("studentEmail", result.getExam().getStudent().getUser().getEmail());
                    map.put("gformScore", result.getGformScore());
                    map.put("tokenValidated", result.getTokenValidated());
                    map.put("generatedToken", result.getGeneratedToken());
                    map.put("studentInputToken", result.getStudentInputToken());
                    map.put("validationStatus", result.getExamValidationStatus().toString());
                    map.put("submissionDate", result.getSubmissionDate());
                    map.put("hasProofPhoto", result.getProofPhotoPath() != null);
                    map.put("proofPhotoPath", result.getProofPhotoPath());
                    map.put("adminNotes", result.getAdminNotes());
                    response.add(map);
                }
            }

            log.info("✅ Retrieved {} exam submissions with status: {}", response.size(), status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting exam submissions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Gagal mengambil data ujian: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get details of a specific exam submission (for modal/pop-out)
     * GET /admin/exam-submissions/{id}
     */
    @GetMapping("/exam-submissions/{id}")
    @PreAuthorize("hasRole('ADMIN_VALIDASI') or hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> getExamSubmissionDetails(@PathVariable Long id) {
        try {
            ExamResult result = examResultRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Exam result not found"));

            Map<String, Object> response = new HashMap<>();
            
            if (result.getExam() != null && result.getExam().getStudent() != null) {
                Student student = result.getExam().getStudent();
                
                response.put("id", result.getId());
                response.put("studentId", student.getId());
                response.put("studentName", student.getFullName());
                response.put("studentEmail", student.getUser().getEmail());
                
                // ✅ Exam submission details
                response.put("studentInputToken", result.getStudentInputToken());
                response.put("generatedToken", result.getGeneratedToken());
                response.put("tokenValidated", result.getTokenValidated());
                response.put("gformScore", result.getGformScore());
                response.put("proofPhotoPath", result.getProofPhotoPath());
                
                // Validation status
                response.put("validationStatus", result.getExamValidationStatus().toString());
                response.put("adminNotes", result.getAdminNotes());
                response.put("submissionDate", result.getSubmissionDate());
                response.put("validatedDate", result.getExamValidatedAt());
                
                // Additional info
                response.put("success", true);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error getting exam submission details: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Gagal mengambil detail ujian: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get Exam Results by Student ID
     * GET /admin/api/exam-results/student/{studentId}
     */
    @GetMapping("/api/exam-results/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN_VALIDASI') or hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> getExamResultByStudentId(@PathVariable Long studentId) {
        try {
            Optional<ExamResult> resultOpt = examResultRepository.findByStudent_Id(studentId);
            
            if (!resultOpt.isPresent()) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(false, "No exam results found for this student"));
            }
            
            ExamResult result = resultOpt.get();
            Map<String, Object> response = new HashMap<>();
            
            response.put("id", result.getId());
            response.put("studentId", studentId);
            if (result.getStudent() != null) {
                response.put("studentName", result.getStudent().getFullName());
                response.put("studentEmail", result.getStudent().getUser().getEmail());
            }
            
            // Exam results
            response.put("score", result.getScore());
            response.put("status", result.getStatus().toString());
            
            // ✅ Student exam submission details
            response.put("studentInputToken", result.getStudentInputToken());
            response.put("generatedToken", result.getGeneratedToken());
            response.put("tokenValidated", result.getTokenValidated());
            response.put("gformScore", result.getGformScore());
            response.put("proofPhotoPath", result.getProofPhotoPath());
            
            // Validation status
            response.put("examValidationStatus", result.getExamValidationStatus() != null ? result.getExamValidationStatus().toString() : null);
            response.put("adminNotes", result.getAdminNotes());
            response.put("submissionDate", result.getSubmissionDate());
            response.put("examValidatedAt", result.getExamValidatedAt());
            
            response.put("success", true);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error getting exam results by student ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Gagal mengambil hasil ujian: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Validate/reject exam submission
     * POST /admin/exam-submissions/{id}/validate
     * {
     *   "action": "APPROVE" or "REJECT",
     *   "adminNotes": "optional notes"
     * }
     */
    @PostMapping("/exam-submissions/{id}/validate")
    @PreAuthorize("hasRole('ADMIN_VALIDASI') or hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> validateExamSubmission(
            @PathVariable Long id,
            @RequestBody ExamValidationRequest request) {
        try {
            String email = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            ExamResult result = examResultRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Exam result not found"));

            // ✅ Key check: Token must match for approval
            if ("APPROVE".equals(request.getAction())) {
                if (!result.getTokenValidated()) {
                    log.warn("❌ [EXAM-FRAUD] Attempt to approve exam {} with invalid token", id);
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "❌ Tidak bisa setujui: Token tidak cocok (indikasi fraud)"));
                }
            }

            if ("APPROVE".equals(request.getAction())) {
                result.setExamValidationStatus(ExamResult.ExamValidationStatus.APPROVED);
                result.setAdminNotes("✅ Disetujui oleh " + admin.getEmail() + " - " + 
                        (request.getAdminNotes() != null ? request.getAdminNotes() : "Lolos validasi"));
                log.info("✅ [EXAM-APPROVED] Exam result {} approved by admin {}", id, email);
            } else if ("REJECT".equals(request.getAction())) {
                result.setExamValidationStatus(ExamResult.ExamValidationStatus.REJECTED);
                result.setAdminNotes("❌ Ditolak oleh " + admin.getEmail() + " - " + 
                        (request.getAdminNotes() != null ? request.getAdminNotes() : 
                         (!result.getTokenValidated() ? "Token tidak cocok" : "Nilai tidak valid")));
                log.info("❌ [EXAM-REJECTED] Exam result {} rejected by admin {}", id, email);
            } else if ("REVISI".equals(request.getAction())) {
                result.setExamValidationStatus(ExamResult.ExamValidationStatus.REVISI);
                result.setAdminNotes("🔄 Diminta revisi oleh " + admin.getEmail() + " - " + 
                        (request.getAdminNotes() != null ? request.getAdminNotes() : "Silahkan perbaiki dan upload ulang"));
                log.info("🔄 [EXAM-REVISI] Exam result {} needs revision per admin {}", id, email);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Action harus APPROVE, REJECT, atau REVISI"));
            }

            result.setExamValidatedAt(LocalDateTime.now());
            result.setValidatedByAdmin(admin);
            examResultRepository.save(result);

            // ✅ Send email notification to student
            try {
                String studentEmail = result.getStudent().getUser().getEmail();
                String studentName = result.getStudent().getFullName();
                String adminNotes = request.getAdminNotes() != null ? request.getAdminNotes() : "";

                if ("APPROVE".equals(request.getAction())) {
                    sendExamApprovalEmail(studentEmail, studentName);
                } else if ("REJECT".equals(request.getAction())) {
                    sendExamRejectionEmail(studentEmail, studentName, adminNotes);
                }
            } catch (Exception emailEx) {
                log.error("⚠️ Email notification failed (exam validation still saved): {}", emailEx.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Validasi ujian berhasil (" + request.getAction() + ")");
            response.put("newStatus", result.getExamValidationStatus().toString());
            response.put("validatedAt", result.getExamValidatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error validating exam: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Gagal validasi ujian: " + e.getMessage()));
        }
    }

    /**
     * ✅ NEW: GET list of re-enrollments awaiting validation
     * GET /admin/api/reenrollments/pending
     */
    @GetMapping("/api/reenrollments/pending")
    @PreAuthorize("hasRole('ADMIN_VALIDASI')")
    public ResponseEntity<?> getPendingReenrollments() {
        try {
            List<ReEnrollment> reenrollments = reenrollmentRepository.findAll().stream()
                    .filter(r -> r.getStatus() == ReEnrollment.ReEnrollmentStatus.SUBMITTED)
                    .toList();

            List<Map<String, Object>> response = new ArrayList<>();
            int[] noArray = {1}; // Use array to make it mutable in inner class
            for (ReEnrollment re : reenrollments) {
                Student student = re.getStudent();
                int totalDocs = re.getDocuments().size();
                int approvedDocs = (int) re.getDocuments().stream()
                        .filter(d -> d.getValidationStatus() == ReEnrollmentDocument.ValidationStatus.APPROVED)
                        .count();

                final int currentNo = noArray[0]++;
                response.add(new HashMap<String, Object>() {{
                    put("no", currentNo);
                    put("id", re.getId());
                    put("studentId", student.getId());
                    put("studentName", student.getFullName());
                    put("email", student.getUser().getEmail());
                    put("programStudi", student.getUser().getEmail()); // Will fetch from formulir later
                    put("totalDocuments", totalDocs);
                    put("approvedDocuments", approvedDocs);
                    put("documentStatus", approvedDocs == totalDocs ? "✅ LENGKAP" : approvedDocs + "/" + totalDocs);
                    put("submittedAt", re.getSubmittedAt());
                    put("status", re.getStatus().toString());
                }});
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching pending reenrollments: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: GET re-enrollment details with documents
     * GET /admin/api/reenrollments/{id}/details
     */
    @GetMapping("/api/reenrollments/{id}/details")
    @PreAuthorize("hasRole('ADMIN_VALIDASI')")
    public ResponseEntity<?> getReerollmentDetails(@PathVariable Long id) {
        try {
            ReEnrollment reenrollment = reenrollmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Re-enrollment tidak ditemukan"));

            Student student = reenrollment.getStudent();
            List<Map<String, Object>> documents = new ArrayList<>();

            // ✅ FIRST: Try loading from ReEnrollmentDocument relationship
            if (reenrollment.getDocuments() != null && !reenrollment.getDocuments().isEmpty()) {
                for (ReEnrollmentDocument doc : reenrollment.getDocuments()) {
                    documents.add(new HashMap<String, Object>() {{
                        put("id", doc.getId());
                        put("documentType", doc.getDocumentType().toString());
                        put("displayName", doc.getDocumentType().getDisplayName());
                        put("fileName", doc.getOriginalFilename());
                        put("fileSize", doc.getFileSize());
                        put("uploadedAt", doc.getUploadedAt());
                        put("validationStatus", doc.getValidationStatus().toString());
                        put("adminNotes", doc.getAdminNotes());
                        put("filePath", doc.getFilePath());
                    }});
                }
                log.info("✅ [REENROLL-DETAILS] Loaded {} documents from documents relationship", documents.size());
            }

            // ✅ SECOND: Fallback - Load from individual file columns if documents empty
            if (documents.isEmpty()) {
                log.info("📄 [REENROLL-DETAILS] Documents relationship empty - loading from individual file columns...");
                
                // Map of file column name -> document type
                Map<String, String> fileMapping = new HashMap<>();
                fileMapping.put("PAKTA_INTEGRITAS", reenrollment.getPaktaIntegritasFile());
                fileMapping.put("IJAZAH", reenrollment.getIjazahFile());
                fileMapping.put("PASPHOTO", reenrollment.getPasphotoFile());
                fileMapping.put("KARTU_KELUARGA", reenrollment.getKartuKeluargaFile());
                fileMapping.put("KARTU_TANDA_PENDUDUK", reenrollment.getKtpFile());
                fileMapping.put("KETERANGAN_BEBAS_NARKOBA", reenrollment.getSuratBebasNarkobaFile());
                fileMapping.put("SKCK", reenrollment.getSkckFile());
                
                for (Map.Entry<String, String> entry : fileMapping.entrySet()) {
                    String docType = entry.getKey();
                    String filePath = entry.getValue();
                    
                    if (filePath != null && !filePath.isEmpty()) {
                        // Get display name from enum
                        String displayName = docType;
                        try {
                            displayName = ReEnrollmentDocument.DocumentType.valueOf(docType).getDisplayName();
                        } catch (IllegalArgumentException e) {
                            log.warn("Unknown document type: {}", docType);
                        }
                        
                        final String finalDisplayName = displayName;
                        final String finalFilePath = filePath;
                        
                        documents.add(new HashMap<String, Object>() {{
                            put("id", null);  // No database ID since it's from column
                            put("documentType", docType);
                            put("displayName", finalDisplayName);
                            put("fileName", new java.io.File(finalFilePath).getName());
                            put("fileSize", null);
                            put("uploadedAt", reenrollment.getSubmittedAt());
                            put("validationStatus", "APPROVED");  // Default to APPROVED for submitted documents
                            put("adminNotes", null);
                            put("filePath", finalFilePath);
                        }});
                        log.info("   ✓ Found: {} -> {}", docType, filePath);
                    }
                }
                log.info("✅ [REENROLL-DETAILS] Fallback loaded {} documents from columns", documents.size());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", reenrollment.getId());
            response.put("studentId", student.getId());
            response.put("studentName", student.getFullName());
            response.put("email", student.getUser().getEmail());
            response.put("parentPhone", reenrollment.getParentPhone());
            response.put("parentEmail", reenrollment.getParentEmail());
            response.put("parentAddress", reenrollment.getParentAddress());
            response.put("permanentAddress", reenrollment.getPermanentAddress());
            response.put("currentAddress", reenrollment.getCurrentAddress());
            response.put("parentName", reenrollment.getParentName());
            response.put("alumniFamily", reenrollment.getAlumniFamily());
            response.put("alumniName", reenrollment.getAlumniName());
            response.put("alumniRelation", reenrollment.getAlumniRelation());
            response.put("submittedAt", reenrollment.getSubmittedAt());
            response.put("status", reenrollment.getStatus().toString());
            response.put("documents", documents);
            response.put("totalDocuments", documents.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching reenrollment details: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Validate individual document
     * PUT /admin/api/reenrollments/documents/{docId}/validate
     */
    @PutMapping("/api/reenrollments/documents/{docId}/validate")
    @PreAuthorize("hasRole('ADMIN_VALIDASI')")
    public ResponseEntity<?> validateDocument(@PathVariable Long docId,
                                              @RequestBody DocumentValidationRequest request) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            ReEnrollmentDocument document = reenrollmentDocumentRepository.findById(docId)
                    .orElseThrow(() -> new RuntimeException("Dokumen tidak ditemukan"));

            if ("APPROVE".equals(request.getAction())) {
                document.setValidationStatus(ReEnrollmentDocument.ValidationStatus.APPROVED);
                document.setAdminNotes(request.getAdminNotes());
                log.info("✅ Document {} approved by admin {}", docId, email);
            } else if ("REJECT".equals(request.getAction())) {
                document.setValidationStatus(ReEnrollmentDocument.ValidationStatus.REJECTED);
                document.setAdminNotes(request.getAdminNotes());
                log.info("❌ Document {} rejected by admin {}", docId, email);
            } else if ("REVISION_NEEDED".equals(request.getAction())) {
                document.setValidationStatus(ReEnrollmentDocument.ValidationStatus.REVISION_NEEDED);
                document.setAdminNotes(request.getAdminNotes());
                log.info("⚠️ Document {} marked for revision by admin {}", docId, email);
            }

            document.setValidatedAt(LocalDateTime.now());
            document.setValidatedByAdmin(admin);
            reenrollmentDocumentRepository.save(document);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Dokumen berhasil divalidasi (" + request.getAction() + ")");
                put("newStatus", document.getValidationStatus().toString());
                put("validatedAt", document.getValidatedAt());
            }});
        } catch (Exception e) {
            log.error("Error validating document: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Finalise re-enrollment validation
     * PUT /admin/api/reenrollments/{id}/finalize
     */
    @PutMapping("/api/reenrollments/{id}/finalize")
    @PreAuthorize("hasRole('ADMIN_VALIDASI')")
    public ResponseEntity<?> finalizeReEnrollment(@PathVariable Long id,
                                                   @RequestBody ReenrollmentFinalizeRequest request) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin tidak ditemukan"));

            ReEnrollment reenrollment = reenrollmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Re-enrollment tidak ditemukan"));

            if ("APPROVE".equals(request.getAction())) {
                // Check all documents are approved
                boolean allApproved = reenrollment.getDocuments().stream()
                        .allMatch(d -> d.getValidationStatus() == ReEnrollmentDocument.ValidationStatus.APPROVED);

                if (!allApproved) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Semua dokumen harus disetujui terlebih dahulu"));
                }

                reenrollment.setStatus(ReEnrollment.ReEnrollmentStatus.VALIDATED);
                reenrollment.setValidationNotes(request.getValidationNotes());
                log.info("✅ Re-enrollment {} approved by admin {}", id, email);
            } else if ("REJECT".equals(request.getAction())) {
                reenrollment.setStatus(ReEnrollment.ReEnrollmentStatus.REJECTED);
                reenrollment.setValidationNotes(request.getValidationNotes());
                log.info("❌ Re-enrollment {} rejected by admin {}", id, email);
            }

            reenrollment.setValidatedAt(LocalDateTime.now());
            reenrollmentRepository.save(reenrollment);

            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("success", true);
                put("message", "Re-enrollment berhasil diproses (" + request.getAction() + ")");
                put("newStatus", reenrollment.getStatus().toString());
                put("validatedAt", reenrollment.getValidatedAt());
            }});
        } catch (Exception e) {
            log.error("Error finalizing reenrollment: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * ✅ DTO untuk validasi dokumen daftar ulang
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DocumentValidationRequest {
        private String action; // APPROVE, REJECT, REVISION_NEEDED
        private String adminNotes;
    }

    /**
     * ✅ DTO untuk finalisasi daftar ulang
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ReenrollmentFinalizeRequest {
        private String action; // APPROVE, REJECT
        private String validationNotes;
    }

    /**
     * ✅ DTO untuk validasi ujian
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExamValidationRequest {
        private String action; // APPROVE, REJECT, or REVISI
        private String adminNotes; // optional notes
    }

    /**
     * ✅ Auto-populate jenis seleksi (Kedokteran & Program Non-Kedokteran)
     */
    @PostMapping("/jenis-seleksi/bulk-initialize")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> bulkInitializeJenisSeleksi() {
        try {
            List<Map<String, Object>> selsToInsert = new ArrayList<>();
            selsToInsert.add(Map.of(
                "code", "KEDOKTERAN", 
                "nama", "Kedokteran", 
                "logoUrl", "💉", 
                "deskripsi", "Program Studi Kedokteran dengan fasilitas praktik lengkap",
                "fasilitas", "Lab Lengkap,Tes tulis,Wawancara,Assessment psikologi",
                "harga", 1500000
            ));
            selsToInsert.add(Map.of(
                "code", "NON_KEDOKTERAN", 
                "nama", "Program Non-Kedokteran", 
                "logoUrl", "🎓", 
                "deskripsi", "Semua program studi non-kedokteran: Teknik, Pendidikan, Ekonomi, Hukum, Seni & Sastra, Pertanian, Psikologi, Sosial-Politik, & Pascasarjana",
                "fasilitas", "Fasilitas Modern,Dosen Bersertifikat,Industri Ready,25+ Program Studi",
                "harga", 750000
            ));
            
            int inserted = 0;
            for (Map<String, Object> sel : selsToInsert) {
                String code = (String) sel.get("code");
                
                // Skip if already exists
                if (jenisSeleksiRepository.existsByCode(code)) {
                    log.info("⏭️ Jenis Seleksi {} sudah ada, skip", code);
                    continue;
                }
                
                JenisSeleksi jenisSeleksi = JenisSeleksi.builder()
                        .code(code)
                        .nama((String) sel.get("nama"))
                        .logoUrl((String) sel.get("logoUrl"))
                        .deskripsi((String) sel.get("deskripsi"))
                        .fasilitas((String) sel.get("fasilitas"))
                        .harga(new BigDecimal((Integer) sel.get("harga")))
                        .isActive(true)
                        .sortOrder(inserted + 1)
                        .build();
                
                jenisSeleksiRepository.save(jenisSeleksi);
                inserted++;
            }
            
            String message = String.format("✅ Jenis Seleksi bulk initialized: %d inserted, %d skipped (already exist)", 
                    inserted, selsToInsert.size() - inserted);
            log.info(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("inserted", inserted);
            response.put("total", selsToInsert.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error bulk initializing jenis seleksi: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ Auto-populate program studi from form-pendaftaran.html
     */
    @PostMapping("/program-studi/bulk-initialize")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> bulkInitializeProgramStudi() {
        try {
            // Clear existing programs if needed (optional)
            // programStudiRepository.deleteAll();
            
            List<Map<String, Object>> programsToInsert = new ArrayList<>();
            
            // KEDOKTERAN programs
            programsToInsert.add(Map.of("kode", "pendidikan-dokter", "nama", "Pendidikan Dokter", "isMedical", true, "sortOrder", 1));
            programsToInsert.add(Map.of("kode", "profesi-dokter", "nama", "Profesi Dokter", "isMedical", true, "sortOrder", 2));
            
            // TEKNIK programs
            programsToInsert.add(Map.of("kode", "teknik-sipil", "nama", "Teknik Sipil", "isMedical", false, "sortOrder", 3));
            programsToInsert.add(Map.of("kode", "teknik-mesin", "nama", "Teknik Mesin", "isMedical", false, "sortOrder", 4));
            programsToInsert.add(Map.of("kode", "teknik-elektro", "nama", "Teknik Elektro", "isMedical", false, "sortOrder", 5));
            programsToInsert.add(Map.of("kode", "informatika", "nama", "Informatika", "isMedical", false, "sortOrder", 6));
            
            // KEGURUAN DAN ILMU PENDIDIKAN programs
            programsToInsert.add(Map.of("kode", "pend-biologi-inggris", "nama", "Pend. Biologi Inggris", "isMedical", false, "sortOrder", 7));
            programsToInsert.add(Map.of("kode", "pend-ekonomi", "nama", "Pend. Ekonomi", "isMedical", false, "sortOrder", 8));
            programsToInsert.add(Map.of("kode", "pend-agama-kristen", "nama", "Pend. Agama Kristen", "isMedical", false, "sortOrder", 9));
            programsToInsert.add(Map.of("kode", "pend-pancasila-kewarganegaraan", "nama", "Pend. Pancasila & Kewarganegaraan", "isMedical", false, "sortOrder", 10));
            programsToInsert.add(Map.of("kode", "pend-bahasa-sastra-indonesia", "nama", "Pend. Bahasa & Sastra Indonesia", "isMedical", false, "sortOrder", 11));
            programsToInsert.add(Map.of("kode", "pend-fisika", "nama", "Pend. Fisika", "isMedical", false, "sortOrder", 12));
            programsToInsert.add(Map.of("kode", "pend-ipa", "nama", "Pend. IPA", "isMedical", false, "sortOrder", 13));
            programsToInsert.add(Map.of("kode", "pend-profesi-guru", "nama", "Pend. Profesi Guru", "isMedical", false, "sortOrder", 14));
            
            // EKONOMI DAN BISNIS programs
            programsToInsert.add(Map.of("kode", "ekonomi-pembangunan", "nama", "Ekonomi Pembangunan", "isMedical", false, "sortOrder", 15));
            programsToInsert.add(Map.of("kode", "manajemen", "nama", "Manajemen", "isMedical", false, "sortOrder", 16));
            programsToInsert.add(Map.of("kode", "akuntansi", "nama", "Akuntansi", "isMedical", false, "sortOrder", 17));
            programsToInsert.add(Map.of("kode", "adm-perpajakan-d3", "nama", "Administrasi Perpajakan (D3)", "isMedical", false, "sortOrder", 18));
            
            int inserted = 0;
            for (Map<String, Object> prog : programsToInsert) {
                String kode = (String) prog.get("kode");
                
                // Skip if already exists
                if (programStudiRepository.existsByKode(kode)) {
                    log.info("⏭️ Program studi {} sudah ada, skip", kode);
                    continue;
                }
                
                ProgramStudi programStudi = ProgramStudi.builder()
                        .kode(kode)
                        .nama((String) prog.get("nama"))
                        .deskripsi("")
                        .isMedical((Boolean) prog.get("isMedical"))
                        .isActive(true)
                        .sortOrder((Integer) prog.get("sortOrder"))
                        .build();
                
                programStudiRepository.save(programStudi);
                inserted++;
            }
            
            String message = String.format("✅ Program studi bulk initialized: %d inserted, %d skipped (already exist)", 
                    inserted, programsToInsert.size() - inserted);
            log.info(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("inserted", inserted);
            response.put("total", programsToInsert.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error bulk initializing program studi: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ Upload Dokumen Sementara (NPM / KTM) untuk mahasiswa
     * POST /admin/api/hasil-akhir/{id}/upload-dokumen
     * Admin validasi upload PDF setelah daftar ulang selesai
     */
    @PostMapping("/api/hasil-akhir/{id}/upload-dokumen")
    @PreAuthorize("hasRole('ADMIN_VALIDASI')")
    public ResponseEntity<?> uploadDokumenSementara(
            @PathVariable Long id,
            @RequestParam(value = "npmSementara", required = false) MultipartFile npmFile,
            @RequestParam(value = "ktmSementara", required = false) MultipartFile ktmFile) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("📄 Admin {} uploading dokumen sementara for HasilAkhir #{}", email, id);

            HasilAkhir hasilAkhir = hasilAkhirRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Hasil Akhir tidak ditemukan"));

            if (npmFile == null && ktmFile == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Minimal satu file harus diupload (NPM atau KTM Sementara)"));
            }

            String uploadDir = "uploads/hasil-akhir/" + hasilAkhir.getStudent().getId();
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            if (npmFile != null && !npmFile.isEmpty()) {
                String originalName = npmFile.getOriginalFilename();
                if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "File NPM Sementara harus berformat PDF"));
                }
                String npmFileName = "npm_sementara_" + System.currentTimeMillis() + ".pdf";
                Path npmPath = uploadPath.resolve(npmFileName);
                Files.copy(npmFile.getInputStream(), npmPath, StandardCopyOption.REPLACE_EXISTING);
                hasilAkhir.setNpmSementaraFile(uploadDir + "/" + npmFileName);
                log.info("✅ NPM Sementara uploaded: {}", npmPath);
            }

            if (ktmFile != null && !ktmFile.isEmpty()) {
                String originalName = ktmFile.getOriginalFilename();
                if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "File KTM Sementara harus berformat PDF"));
                }
                String ktmFileName = "ktm_sementara_" + System.currentTimeMillis() + ".pdf";
                Path ktmPath = uploadPath.resolve(ktmFileName);
                Files.copy(ktmFile.getInputStream(), ktmPath, StandardCopyOption.REPLACE_EXISTING);
                hasilAkhir.setKtmSementaraFile(uploadDir + "/" + ktmFileName);
                log.info("✅ KTM Sementara uploaded: {}", ktmPath);
            }

            hasilAkhirRepository.save(hasilAkhir);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Dokumen sementara berhasil diupload");
            result.put("npmSementaraFile", hasilAkhir.getNpmSementaraFile());
            result.put("ktmSementaraFile", hasilAkhir.getKtmSementaraFile());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error uploading dokumen sementara: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ Get all HasilAkhir with student info for admin dokumen upload
     * GET /admin/api/hasil-akhir/all
     */
    @GetMapping("/api/hasil-akhir/all")
    @PreAuthorize("hasRole('ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllHasilAkhir() {
        try {
            List<HasilAkhir> allHasilAkhir = hasilAkhirRepository.findAll();
            List<Map<String, Object>> results = new ArrayList<>();

            for (HasilAkhir ha : allHasilAkhir) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", ha.getId());
                item.put("studentName", ha.getStudent().getFullName());
                item.put("nomorRegistrasi", ha.getNomorRegistrasi());
                item.put("programStudi", ha.getProgramStudiName());
                item.put("status", ha.getStatus().toString());
                item.put("npmSementaraFile", ha.getNpmSementaraFile());
                item.put("ktmSementaraFile", ha.getKtmSementaraFile());
                results.add(item);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("❌ Error getting all hasil akhir: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }
}


