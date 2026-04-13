package com.uhn.pmb.component;

import com.uhn.pmb.entity.JenisSeleksi;
import com.uhn.pmb.entity.ProgramStudi;
import com.uhn.pmb.entity.User;
import com.uhn.pmb.entity.RegistrationPeriod;
import com.uhn.pmb.entity.SelectionProgramStudi;
import com.uhn.pmb.entity.PeriodJenisSeleksi;
import com.uhn.pmb.entity.FormulaSelection;
import com.uhn.pmb.entity.SelectionType;
import com.uhn.pmb.repository.JenisSeleksiRepository;
import com.uhn.pmb.repository.ProgramStudiRepository;
import com.uhn.pmb.repository.UserRepository;
import com.uhn.pmb.repository.RegistrationPeriodRepository;
import com.uhn.pmb.repository.SelectionProgramStudiRepository;
import com.uhn.pmb.repository.PeriodJenisSeleksiRepository;
import com.uhn.pmb.repository.FormulaSelectionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Auto-initialize master data on application startup
 * - Check if data exists
 * - If empty, insert default values
 * - This ensures fresh deployments have baseline data
 */
@Component
@RequiredArgsConstructor
@Slf4j
class MasterDataInitializer implements ApplicationRunner {

    private final JenisSeleksiRepository jenisSeleksiRepository;
    private final ProgramStudiRepository programStudiRepository;
    private final UserRepository userRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final SelectionProgramStudiRepository selectionProgramStudiRepository;
    private final PeriodJenisSeleksiRepository periodJenisSeleksiRepository;
    private final FormulaSelectionRepository formulaSelectionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("🚀 [DATA-INITIALIZER] Starting application startup data initialization...");
        
        try {
            initializeUsers();
            initializeRegistrationPeriods();
            initializeFormulaSelection();
            initializeJenisSeleksi();
            initializeProgramStudi();
            initializeSelectionProgramStudi();
            initializePeriodJenisSeleksi();
            log.info("✅ [DATA-INITIALIZER] Data initialization completed successfully");
        } catch (Exception e) {
            log.error("❌ [DATA-INITIALIZER] Error during data initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize Formula Selection if empty
     * (Formula Selection are the academic program pathways shown at registration step 2)
     */
    private void initializeFormulaSelection() {
        long count = formulaSelectionRepository.count();
        
        if (count > 0) {
            log.info("ℹ️ [FORMULA-SELECTION] Already has {} formula selections, skipping initialization", count);
            return;
        }

        log.info("📝 [FORMULA-SELECTION] Database empty, initializing default formula selections...");
        
        List<FormulaSelection> defaultFormulas = new ArrayList<>();
        
        // MEDICAL Program (Kedokteran)
        defaultFormulas.add(FormulaSelection.builder()
                .code("MEDICAL")
                .title("Kedokteran")
                .description("Program Studi Kedokteran dengan fasilitas praktik lengkap dan kurikulum internasional")
                .iconEmoji("🏥")
                .price(new BigDecimal(1500000))
                .formType(SelectionType.FormType.MEDICAL)
                .features("Lab Lengkap,Tes Tulis,Wawancara,Assessment Psikologi,Simulasi Klinis")
                .isActive(true)
                .sortOrder(1)
                .build());
        
        // NON-MEDICAL Programs (All other programs)
        defaultFormulas.add(FormulaSelection.builder()
                .code("NON_MEDICAL")
                .title("Program Non-Kedokteran")
                .description("25+ Program Studi Non-Medis: Teknik, Pendidikan, Ekonomi, Hukum, Psikologi, Pertanian, dan Pascasarjana")
                .iconEmoji("📚")
                .price(new BigDecimal(750000))
                .formType(SelectionType.FormType.NON_MEDICAL)
                .features("Fasilitas Modern,Dosen Bersertifikat,Industri Ready,25+ Program Studi,Tools Canggih")
                .isActive(true)
                .sortOrder(2)
                .build());
        
        formulaSelectionRepository.saveAll(defaultFormulas);
        log.info("✅ [FORMULA-SELECTION] Initialized {} default formula selections", defaultFormulas.size());
    }

    /**
     * Initialize Jenis Seleksi if empty
     */
    private void initializeJenisSeleksi() {
        long count = jenisSeleksiRepository.count();
        
        if (count > 0) {
            log.info("ℹ️ [JENIS-SELEKSI] Already has {} jenis seleksi, skipping initialization", count);
            return;
        }

        log.info("📝 [JENIS-SELEKSI] Database empty, initializing default jenis seleksi...");
        
        List<JenisSeleksi> defaultSelections = new ArrayList<>();
        
        // Kedokteran
        defaultSelections.add(JenisSeleksi.builder()
                .code("KEDOKTERAN")
                .nama("Kedokteran")
                .logoUrl("💉")
                .deskripsi("Program Studi Kedokteran dengan fasilitas praktik lengkap")
                .fasilitas("Lab Lengkap,Tes tulis,Wawancara,Assessment psikologi")
                .harga(new BigDecimal(1500000))
                .isActive(true)
                .sortOrder(1)
                .build());
        
        // Program Non-Kedokteran
        defaultSelections.add(JenisSeleksi.builder()
                .code("NON_KEDOKTERAN")
                .nama("Program Non-Kedokteran")
                .logoUrl("🎓")
                .deskripsi("Semua program studi non-kedokteran: Teknik, Pendidikan, Ekonomi, Hukum, Seni & Sastra, Pertanian, Psikologi, Sosial-Politik, & Pascasarjana")
                .fasilitas("Fasilitas Modern,Dosen Bersertifikat,Industri Ready,25+ Program Studi")
                .harga(new BigDecimal(750000))
                .isActive(true)
                .sortOrder(2)
                .build());
        
        jenisSeleksiRepository.saveAll(defaultSelections);
        log.info("✅ [JENIS-SELEKSI] Initialized {} default jenis seleksi", defaultSelections.size());
    }

    /**
     * Initialize Program Studi if empty
     */
    private void initializeProgramStudi() {
        long count = programStudiRepository.count();
        
        if (count > 0) {
            log.info("ℹ️ [PROGRAM-STUDI] Already has {} program studi, skipping initialization", count);
            return;
        }

        log.info("📝 [PROGRAM-STUDI] Database empty, initializing 25 program studi with real tuition fees...");
        
        List<ProgramStudi> defaultPrograms = new ArrayList<>();
        int sortOrder = 1;

        // FKIP (Faculty of Teacher Training & Education)
        defaultPrograms.add(createProgramStudi("pend-fisika", "Pend. Fisika", false, sortOrder++, 4305500L, 1236583L));
        defaultPrograms.add(createProgramStudi("pend-bahasa-sastra-indonesia", "Pend. B. Indonesia", false, sortOrder++, 8999000L, 1596567L));
        defaultPrograms.add(createProgramStudi("pend-biologi-inggris", "Pend. B. Inggris", false, sortOrder++, 8980000L, 1938867L));
        defaultPrograms.add(createProgramStudi("pend-pancasila-kewarganegaraan", "Pend. PPKn", false, sortOrder++, 7875000L, 1821267L));
        defaultPrograms.add(createProgramStudi("pend-ekonomi", "Pend. Ekonomi", false, sortOrder++, 8057000L, 1875867L));
        defaultPrograms.add(createProgramStudi("pend-matematika", "Pend. Matematika", false, sortOrder++, 7968000L, 1875867L));
        defaultPrograms.add(createProgramStudi("pend-agama-kristen", "Pend. Agama Kristen", false, sortOrder++, 7966000L, 1848567L));
        defaultPrograms.add(createProgramStudi("pend-ipa", "Pend. IPA", false, sortOrder++, 4782500L, 1275083L));

        // FISIPOL (Faculty of Social & Political Sciences)
        defaultPrograms.add(createProgramStudi("adm-bisnis", "Adm. Bisnis", false, sortOrder++, 7433000L, 1719967L));
        defaultPrograms.add(createProgramStudi("adm-publik", "Adm. Publik", false, sortOrder++, 5712250L, 1427475L));

        // TEKNIK (Engineering Faculty)
        defaultPrograms.add(createProgramStudi("teknik-sipil", "Teknik Sipil", false, sortOrder++, 9921000L, 2280167L));
        defaultPrograms.add(createProgramStudi("teknik-mesin", "Teknik Mesin", false, sortOrder++, 10131000L, 2352167L));
        defaultPrograms.add(createProgramStudi("teknik-elektro", "Teknik Elektro", false, sortOrder++, 5438000L, 1455583L));
        defaultPrograms.add(createProgramStudi("informatika", "Informatika", false, sortOrder++, 8391000L, 2091167L));

        // PETERNAKAN (Animal Science)
        defaultPrograms.add(createProgramStudi("prod-ternak", "Prod. Ternak", false, sortOrder++, 6438250L, 1615125L));

        // EKONOMI & BISNIS (Economics & Business)
        defaultPrograms.add(createProgramStudi("akuntansi", "Akuntansi", false, sortOrder++, 9691000L, 2111167L));
        defaultPrograms.add(createProgramStudi("manajemen", "Manajemen", false, sortOrder++, 9991000L, 2111167L));
        defaultPrograms.add(createProgramStudi("ekonomi-pembangunan", "Ekonomi Pembangunan", false, sortOrder++, 8827000L, 1981567L));
        defaultPrograms.add(createProgramStudi("adm-pajak", "Adm. Pajak", false, sortOrder++, 8027000L, 1896567L));

        // HUKUM (Law)
        defaultPrograms.add(createProgramStudi("ilmu-hukum", "Ilmu Hukum", false, sortOrder++, 8355000L, 1857967L));

        // PERTANIAN (Agriculture)
        defaultPrograms.add(createProgramStudi("agroteknologi", "Agroteknologi", false, sortOrder++, 6475750L, 1933875L));
        defaultPrograms.add(createProgramStudi("agribisnis", "Agribisnis", false, sortOrder++, 6475750L, 1933875L));

        // BAHASA & SENI (Language & Arts)
        defaultPrograms.add(createProgramStudi("seni-musik", "Seni Musik", false, sortOrder++, 4338500L, 1248283L));
        defaultPrograms.add(createProgramStudi("sastra-inggris", "Sastra Inggris", false, sortOrder++, 6558250L, 1860625L));

        // PSIKOLOGI (Psychology)
        defaultPrograms.add(createProgramStudi("psikologi", "Psikologi", false, sortOrder++, 6475500L, 1546200L));

        programStudiRepository.saveAll(defaultPrograms);
        log.info("✅ [PROGRAM-STUDI] Initialized {} program studi with real tuition fees", defaultPrograms.size());
    }

    /**
     * Helper method to create ProgramStudi entity
     */
    private ProgramStudi createProgramStudi(String kode, String nama, boolean isMedical, int sortOrder, Long hargaTotalPerTahun, Long cicilan1) {
        return ProgramStudi.builder()
                .kode(kode)
                .nama(nama)
                .deskripsi("")
                .isMedical(isMedical)
                .isActive(true)
                .sortOrder(sortOrder)
                .hargaTotalPerTahun(hargaTotalPerTahun)
                .cicilan1(cicilan1)
                .build();
    }

    /**
     * Initialize Users if empty
     */
    private void initializeUsers() {
        long count = userRepository.count();
        
        if (count > 0) {
            log.info("ℹ️ [USERS] Already has {} users, skipping initialization", count);
            return;
        }

        log.info("📝 [USERS] Database empty, initializing 3 default users...");
        
        List<User> defaultUsers = new ArrayList<>();
        
        // Admin Pusat
        defaultUsers.add(User.builder()
                .email("admin@pmb.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.UserRole.ADMIN_PUSAT)
                .isActive(true)
                .build());
        
        // Admin Validasi
        defaultUsers.add(User.builder()
                .email("validasi@pmb.com")
                .password(passwordEncoder.encode("validasi123"))
                .role(User.UserRole.ADMIN_VALIDASI)
                .isActive(true)
                .build());
        
        // Camaba (student)
        defaultUsers.add(User.builder()
                .email("camaba@pmb.com")
                .password(passwordEncoder.encode("camaba123"))
                .role(User.UserRole.CAMABA)
                .isActive(true)
                .build());
        
        userRepository.saveAll(defaultUsers);
        log.info("✅ [USERS] Initialized {} default users", defaultUsers.size());
    }

    /**
     * Initialize Registration Periods (Gelombang) if empty
     */
    private void initializeRegistrationPeriods() {
        long count = registrationPeriodRepository.count();
        
        if (count > 0) {
            log.info("ℹ️ [GELOMBANG] Already has {} registration periods, skipping initialization", count);
            return;
        }

        log.info("📝 [GELOMBANG] Database empty, initializing 3 default registration periods...");
        
        List<RegistrationPeriod> defaultPeriods = new ArrayList<>();
        
        // Gelombang Awal (Early - No Test)
        defaultPeriods.add(RegistrationPeriod.builder()
                .name("Gelombang Awal")
                .waveType(RegistrationPeriod.WaveType.EARLY_NO_TEST)
                .regStartDate(java.time.LocalDateTime.of(2026, 4, 1, 0, 0))
                .regEndDate(java.time.LocalDateTime.of(2026, 4, 30, 23, 59))
                .examDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .examEndDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .announcementDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .reenrollmentStartDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .reenrollmentEndDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .description("Gelombang awal tanpa tes tertulis - Seleksi berdasarkan nilai rapor")
                .status(RegistrationPeriod.Status.OPEN)
                .build());
        
        // Gelombang Reguler (Regular - With Test)
        defaultPeriods.add(RegistrationPeriod.builder()
                .name("Gelombang Reguler")
                .waveType(RegistrationPeriod.WaveType.REGULAR_TEST)
                .regStartDate(java.time.LocalDateTime.of(2026, 5, 1, 0, 0))
                .regEndDate(java.time.LocalDateTime.of(2026, 5, 31, 23, 59))
                .examDate(java.time.LocalDateTime.of(2026, 6, 15, 8, 0))
                .examEndDate(java.time.LocalDateTime.of(2026, 6, 15, 12, 0))
                .announcementDate(java.time.LocalDateTime.of(2026, 6, 25, 10, 0))
                .reenrollmentStartDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .reenrollmentEndDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .description("Gelombang reguler dengan tes tertulis dan wawancara")
                .status(RegistrationPeriod.Status.OPEN)
                .build());
        
        // Gelombang Ranking (Ranking - No Test)
        defaultPeriods.add(RegistrationPeriod.builder()
                .name("Gelombang Ranking")
                .waveType(RegistrationPeriod.WaveType.RANKING_NO_TEST)
                .regStartDate(java.time.LocalDateTime.of(2026, 6, 26, 0, 0))
                .regEndDate(java.time.LocalDateTime.of(2026, 6, 30, 23, 59))
                .examDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .examEndDate(java.time.LocalDateTime.of(2026, 12, 31, 23, 59))
                .announcementDate(java.time.LocalDateTime.of(2026, 7, 10, 10, 0))
                .reenrollmentStartDate(java.time.LocalDateTime.of(2026, 7, 11, 0, 0))
                .reenrollmentEndDate(java.time.LocalDateTime.of(2026, 7, 15, 23, 59))
                .description("Gelombang ranking - Pendaftar tersisa dari gelombang sebelumnya")
                .status(RegistrationPeriod.Status.OPEN)
                .build());
        
        registrationPeriodRepository.saveAll(defaultPeriods);
        log.info("✅ [GELOMBANG] Initialized {} default registration periods", defaultPeriods.size());
    }

    /**
     * Initialize SelectionProgramStudi (junction: KEDOKTERAN ↔ Program Dokter, NON_KEDOKTERAN ↔ Non-Dokter)
     */
    private void initializeSelectionProgramStudi() {
        if (selectionProgramStudiRepository.count() > 0) {
            log.info("ℹ️ [SELECTION-PROGRAM] Already has data, skipping initialization");
            return;
        }

        log.info("📝 [SELECTION-PROGRAM] Initializing selection program studi relationships...");

        // Find KEDOKTERAN and NON_KEDOKTERAN jenis seleksi
        JenisSeleksi kedokteran = jenisSeleksiRepository.findByCode("KEDOKTERAN").orElse(null);
        JenisSeleksi nonKedokteran = jenisSeleksiRepository.findByCode("NON_KEDOKTERAN").orElse(null);

        // Get all programs
        List<ProgramStudi> allPrograms = programStudiRepository.findAll();
        List<SelectionProgramStudi> relationships = new ArrayList<>();

        // Link KEDOKTERAN to medical programs only
        if (kedokteran != null) {
            allPrograms.stream()
                    .filter(p -> p.getIsMedical() != null && p.getIsMedical())
                    .forEach(program -> {
                        relationships.add(SelectionProgramStudi.builder()
                                .jenisSeleksi(kedokteran)
                                .programStudi(program)
                                .isActive(true)
                                .build());
                    });
            log.info("✅ [SELECTION-PROGRAM] Linked KEDOKTERAN to {} medical programs", 
                    relationships.size());
        }

        // Link NON_KEDOKTERAN to all non-medical programs
        if (nonKedokteran != null) {
            allPrograms.stream()
                    .filter(p -> p.getIsMedical() == null || !p.getIsMedical())
                    .forEach(program -> {
                        relationships.add(SelectionProgramStudi.builder()
                                .jenisSeleksi(nonKedokteran)
                                .programStudi(program)
                                .isActive(true)
                                .build());
                    });
            log.info("✅ [SELECTION-PROGRAM] Linked NON_KEDOKTERAN to non-medical programs");
        }

        selectionProgramStudiRepository.saveAll(relationships);
        log.info("✅ [SELECTION-PROGRAM] Initialized {} selection-program relationships", relationships.size());
    }

    /**
     * Initialize PeriodJenisSeleksi (junction: Gelombang ↔ KEDOKTERAN + NON_KEDOKTERAN)
     */
    private void initializePeriodJenisSeleksi() {
        if (periodJenisSeleksiRepository.count() > 0) {
            log.info("ℹ️ [PERIOD-JENIS-SELEKSI] Already has data, skipping initialization");
            return;
        }

        log.info("📝 [PERIOD-JENIS-SELEKSI] Initializing period jenis seleksi relationships...");

        // Find jenis seleksi
        JenisSeleksi kedokteran = jenisSeleksiRepository.findByCode("KEDOKTERAN").orElse(null);
        JenisSeleksi nonKedokteran = jenisSeleksiRepository.findByCode("NON_KEDOKTERAN").orElse(null);

        // Get all periods
        List<RegistrationPeriod> allPeriods = registrationPeriodRepository.findAll();
        List<PeriodJenisSeleksi> relationships = new ArrayList<>();

        // For each period, add both jenis seleksi
        if (kedokteran != null && nonKedokteran != null) {
            allPeriods.forEach(period -> {
                // Add KEDOKTERAN
                relationships.add(PeriodJenisSeleksi.builder()
                        .period(period)
                        .jenisSeleksi(kedokteran)
                        .isActive(true)
                        .build());

                // Add NON_KEDOKTERAN
                relationships.add(PeriodJenisSeleksi.builder()
                        .period(period)
                        .jenisSeleksi(nonKedokteran)
                        .isActive(true)
                        .build());
            });
            
            log.info("✅ [PERIOD-JENIS-SELEKSI] Linked each of {} periods to 2 jenis seleksi", allPeriods.size());
        }

        periodJenisSeleksiRepository.saveAll(relationships);
        log.info("✅ [PERIOD-JENIS-SELEKSI] Initialized {} period-jenis-seleksi relationships", relationships.size());
    }
}
