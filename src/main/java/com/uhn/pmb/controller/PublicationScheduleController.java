package com.uhn.pmb.controller;

import com.uhn.pmb.entity.PublicationSchedule;
import com.uhn.pmb.entity.RegistrationPeriod;
import com.uhn.pmb.repository.PublicationScheduleRepository;
import com.uhn.pmb.repository.RegistrationPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin/api/publication-schedule")
@RequiredArgsConstructor
@Slf4j
public class PublicationScheduleController {

    private final PublicationScheduleRepository publicationScheduleRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;

    /**
     * Get all publication schedules
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getAllSchedules() {
        List<PublicationSchedule> schedules = publicationScheduleRepository.findAllByOrderByPublishDateTimeDesc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (PublicationSchedule s : schedules) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("periodId", s.getPeriod().getId());
            map.put("periodName", s.getPeriod().getName());
            map.put("publishDateTime", s.getPublishDateTime().toString());
            map.put("isPublished", s.getIsPublished());
            map.put("publishedAt", s.getPublishedAt() != null ? s.getPublishedAt().toString() : null);
            map.put("createdBy", s.getCreatedBy());
            map.put("notes", s.getNotes());
            map.put("resultsVisible", s.isResultsVisible());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Get schedule for a specific period
     */
    @GetMapping("/{periodId}")
    @PreAuthorize("hasAnyRole('ADMIN_PUSAT', 'ADMIN_VALIDASI')")
    public ResponseEntity<?> getScheduleByPeriod(@PathVariable Long periodId) {
        Optional<PublicationSchedule> schedule = publicationScheduleRepository.findByPeriodId(periodId);
        if (schedule.isEmpty()) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
        PublicationSchedule s = schedule.get();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("exists", true);
        map.put("id", s.getId());
        map.put("periodId", s.getPeriod().getId());
        map.put("periodName", s.getPeriod().getName());
        map.put("publishDateTime", s.getPublishDateTime().toString());
        map.put("isPublished", s.getIsPublished());
        map.put("publishedAt", s.getPublishedAt() != null ? s.getPublishedAt().toString() : null);
        map.put("notes", s.getNotes());
        map.put("resultsVisible", s.isResultsVisible());
        return ResponseEntity.ok(map);
    }

    /**
     * Create or update publication schedule
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> createOrUpdateSchedule(@RequestBody Map<String, Object> request,
                                                     Authentication auth) {
        Long periodId = Long.valueOf(request.get("periodId").toString());
        String publishDateTimeStr = request.get("publishDateTime").toString();
        String notes = request.containsKey("notes") ? (String) request.get("notes") : null;

        RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        LocalDateTime publishDateTime = LocalDateTime.parse(publishDateTimeStr);

        PublicationSchedule schedule = publicationScheduleRepository.findByPeriodId(periodId)
                .orElse(PublicationSchedule.builder()
                        .period(period)
                        .isPublished(false)
                        .build());

        schedule.setPublishDateTime(publishDateTime);
        schedule.setNotes(notes);
        schedule.setCreatedBy(auth.getName());

        // Auto-publish if scheduled time has passed
        if (LocalDateTime.now().isAfter(publishDateTime) && !Boolean.TRUE.equals(schedule.getIsPublished())) {
            schedule.setIsPublished(true);
            schedule.setPublishedAt(LocalDateTime.now());
        }

        schedule = publicationScheduleRepository.save(schedule);
        log.info("📅 [PUB-SCHEDULE] Schedule saved for period {} at {}", period.getName(), publishDateTime);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Jadwal publikasi berhasil disimpan",
                "id", schedule.getId()
        ));
    }

    /**
     * Manually publish results now (override schedule)
     */
    @PostMapping("/{periodId}/publish-now")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> publishNow(@PathVariable Long periodId, Authentication auth) {
        RegistrationPeriod period = registrationPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Period not found"));

        PublicationSchedule schedule = publicationScheduleRepository.findByPeriodId(periodId)
                .orElse(PublicationSchedule.builder()
                        .period(period)
                        .publishDateTime(LocalDateTime.now())
                        .build());

        schedule.setIsPublished(true);
        schedule.setPublishedAt(LocalDateTime.now());
        schedule.setCreatedBy(auth.getName());
        publicationScheduleRepository.save(schedule);

        log.info("📢 [PUB-SCHEDULE] Results published NOW for period {} by {}", period.getName(), auth.getName());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hasil kelulusan berhasil dipublikasikan"
        ));
    }

    /**
     * Delete schedule
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PUSAT')")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        publicationScheduleRepository.deleteById(id);
        log.info("🗑️ [PUB-SCHEDULE] Schedule {} deleted", id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Jadwal dihapus"));
    }
}
