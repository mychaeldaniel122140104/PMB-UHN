package com.uhn.pmb.service;

import com.uhn.pmb.entity.*;
import com.uhn.pmb.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ValidationStatusTrackerService {

    @Autowired
    private ValidationStatusTrackerRepository validationStatusTrackerRepository;

    @Autowired
    private AdmissionFormRepository admissionFormRepository;

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Create atau get ValidationStatusTracker untuk admission form
     */
    @Transactional
    public ValidationStatusTracker getOrCreateTracker(Long admissionFormId) {
        Optional<ValidationStatusTracker> existing = validationStatusTrackerRepository.findByAdmissionFormId(admissionFormId);
        if (existing.isPresent()) {
            return existing.get();
        }

        AdmissionForm form = admissionFormRepository.findById(admissionFormId)
                .orElseThrow(() -> new RuntimeException("AdmissionForm not found"));

        ValidationStatusTracker tracker = ValidationStatusTracker.builder()
                .admissionForm(form)
                .student(form.getStudent())
                .status(ValidationStatusTracker.ValidationStatusEnum.NOT_STARTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return validationStatusTrackerRepository.save(tracker);
    }

    /**
     * Update status saat pembayaran berhasil
     */
    @Transactional
    public void updateStatusToMenunggu(Long admissionFormId) {
        ValidationStatusTracker tracker = getOrCreateTracker(admissionFormId);
        tracker.setStatus(ValidationStatusTracker.ValidationStatusEnum.MENUNGGU);
        tracker.setLastAction("PAYMENT_VERIFIED");
        tracker.setLastReason("Pembayaran terverifikasi");
        tracker.setUpdatedAt(LocalDateTime.now());
        validationStatusTrackerRepository.save(tracker);
    }

    /**
     * Update status saat di-approve oleh validasi
     */
    @Transactional
    public void updateStatusToDivalidasi(Long admissionFormId, User validatedBy) {
        ValidationStatusTracker tracker = getOrCreateTracker(admissionFormId);
        tracker.setStatus(ValidationStatusTracker.ValidationStatusEnum.DIVALIDASI);
        tracker.setLastAction("APPROVED");
        tracker.setLastReason("Formulir disetujui oleh validasi");
        tracker.setLastUpdatedBy(validatedBy);
        tracker.setUpdatedAt(LocalDateTime.now());
        validationStatusTrackerRepository.save(tracker);
    }

    /**
     * Update status saat ditolak oleh validasi
     */
    @Transactional
    public void updateStatusToDitolak(Long admissionFormId, String reason, User rejectedBy) {
        ValidationStatusTracker tracker = getOrCreateTracker(admissionFormId);
        tracker.setStatus(ValidationStatusTracker.ValidationStatusEnum.DITOLAK);
        tracker.setLastAction("REJECTED");
        tracker.setLastReason(reason);
        tracker.setLastUpdatedBy(rejectedBy);
        tracker.setUpdatedAt(LocalDateTime.now());
        validationStatusTrackerRepository.save(tracker);
    }

    /**
     * Update status saat diminta revisi oleh validasi
     */
    @Transactional
    public void updateStatusToRevisi(Long admissionFormId, String reason, User requestedBy) {
        ValidationStatusTracker tracker = getOrCreateTracker(admissionFormId);
        tracker.setStatus(ValidationStatusTracker.ValidationStatusEnum.REVISI);
        tracker.setLastAction("REVISION_REQUESTED");
        tracker.setLastReason(reason);
        tracker.setLastUpdatedBy(requestedBy);
        tracker.setUpdatedAt(LocalDateTime.now());
        validationStatusTrackerRepository.save(tracker);
    }

    /**
     * Get current validation status
     */
    public Optional<ValidationStatusTracker> getTrackerByFormId(Long admissionFormId) {
        return validationStatusTrackerRepository.findByAdmissionFormId(admissionFormId);
    }

    /**
     * Get current validation status by student
     */
    public Optional<ValidationStatusTracker> getTrackerByStudentId(Long studentId) {
        return validationStatusTrackerRepository.findByStudentId(studentId);
    }
}
