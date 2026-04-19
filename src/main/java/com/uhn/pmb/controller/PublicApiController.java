package com.uhn.pmb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uhn.pmb.entity.RegistrationPeriod;
import com.uhn.pmb.entity.JenisSeleksi;
import com.uhn.pmb.entity.ProgramStudi;
import com.uhn.pmb.entity.PublicationSchedule;
import com.uhn.pmb.repository.*;
import com.uhn.pmb.dto.ApiResponse;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PublicApiController {

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private JenisSeleksiRepository jenisSeleksiRepository;

    @Autowired
    private ProgramStudiRepository programStudiRepository;

    @Autowired
    private PublicationScheduleRepository publicationScheduleRepository;

    @GetMapping("/gelombang")
    public ResponseEntity<?> getAllGelombang() {
        try {
            List<RegistrationPeriod> data = registrationPeriodRepository.findAll().stream()
                .sorted(Comparator.comparing(RegistrationPeriod::getRegStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ApiResponse(false, "Error loading gelombang: " + e.getMessage()));
        }
    }

    @GetMapping("/jenis-seleksi")
    public ResponseEntity<?> getAllJenisSeleksi() {
        try {
            List<JenisSeleksi> data = jenisSeleksiRepository.findAll().stream()
                .filter(js -> js.getIsActive() == null || js.getIsActive())
                .sorted(Comparator.comparing(js -> js.getSortOrder() != null ? js.getSortOrder() : 999))
                .collect(Collectors.toList());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ApiResponse(false, "Error loading jenis seleksi: " + e.getMessage()));
        }
    }

    @GetMapping("/program-studi")
    public ResponseEntity<?> getAllProgramStudi() {
        try {
            List<ProgramStudi> data = programStudiRepository.findAll().stream()
                .filter(ps -> ps.getIsActive() == null || ps.getIsActive())
                .sorted(Comparator.comparing(ps -> ps.getSortOrder() != null ? ps.getSortOrder() : 999))
                .collect(Collectors.toList());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ApiResponse(false, "Error loading program studi: " + e.getMessage()));
        }
    }

    @GetMapping("/program-studi/{id}")
    public ResponseEntity<?> getProgramStudiById(@PathVariable Long id) {
        try {
            Optional<ProgramStudi> data = programStudiRepository.findById(id);
            if (data.isPresent()) {
                return ResponseEntity.ok(data.get());
            }
            return ResponseEntity.ok(new ApiResponse(false, "Program studi tidak ditemukan"));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "Error loading program studi: " + e.getMessage()));
        }
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getPublicSettings() {
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("videoTutorialUrl", "https://www.youtube.com/embed/dQw4w9WgXcQ");
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "Error loading settings: " + e.getMessage()));
        }
    }

    /**
     * F013: Check if results are published for a specific period (time-gated)
     */
    @GetMapping("/publication-status/{periodId}")
    public ResponseEntity<?> getPublicationStatus(@PathVariable Long periodId) {
        try {
            Optional<PublicationSchedule> schedule = publicationScheduleRepository.findByPeriodId(periodId);
            Map<String, Object> result = new HashMap<>();

            if (schedule.isPresent()) {
                PublicationSchedule s = schedule.get();
                result.put("hasSchedule", true);
                result.put("publishDateTime", s.getPublishDateTime().toString());
                result.put("isPublished", s.getIsPublished());
                result.put("resultsVisible", s.isResultsVisible());
            } else {
                result.put("hasSchedule", false);
                result.put("resultsVisible", false);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * F031: Get distinct fakultas list
     */
    @GetMapping("/fakultas")
    public ResponseEntity<?> getAllFakultas() {
        try {
            List<String> fakultasList = programStudiRepository.findDistinctFakultasActive();
            return ResponseEntity.ok(fakultasList);
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * F031: Get program studi grouped by fakultas
     */
    @GetMapping("/program-studi/by-fakultas")
    public ResponseEntity<?> getProgramStudiByFakultas() {
        try {
            List<ProgramStudi> allActive = programStudiRepository.findByIsActiveTrueOrderBySortOrder();
            Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();

            for (ProgramStudi ps : allActive) {
                String fak = ps.getFakultas() != null ? ps.getFakultas() : "Lainnya";
                grouped.computeIfAbsent(fak, k -> new ArrayList<>());

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", ps.getId());
                item.put("kode", ps.getKode());
                item.put("nama", ps.getNama());
                item.put("fakultas", fak);
                item.put("hargaTotalPerTahun", ps.getHargaTotalPerTahun());
                grouped.get(fak).add(item);
            }
            return ResponseEntity.ok(grouped);
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "Error: " + e.getMessage()));
        }
    }
}
