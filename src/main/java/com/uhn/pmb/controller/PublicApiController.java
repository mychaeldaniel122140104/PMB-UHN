package com.uhn.pmb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.uhn.pmb.entity.RegistrationPeriod;
import com.uhn.pmb.entity.JenisSeleksi;
import com.uhn.pmb.entity.ProgramStudi;
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
}
