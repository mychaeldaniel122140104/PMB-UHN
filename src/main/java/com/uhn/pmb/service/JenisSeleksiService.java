package com.uhn.pmb.service;

import com.uhn.pmb.entity.JenisSeleksi;
import com.uhn.pmb.repository.JenisSeleksiRepository;
import com.uhn.pmb.repository.PeriodJenisSeleksiRepository;
import com.uhn.pmb.repository.SelectionProgramStudiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class JenisSeleksiService {

    private final JenisSeleksiRepository jenisSeleksiRepository;
    private final PeriodJenisSeleksiRepository periodJenisSeleksiRepository;
    private final SelectionProgramStudiRepository selectionProgramStudiRepository;
    private final EntityManager entityManager;

    /**
     * Get all active jenis seleksi ordered by sort order
     */
    public List<JenisSeleksi> getAllActive() {
        return jenisSeleksiRepository.findByIsActiveTrueOrderBySortOrder();
    }

    /**
     * Get all jenis seleksi (including inactive) ordered by sort order
     */
    public List<JenisSeleksi> getAll() {
        return jenisSeleksiRepository.findAllByOrderBySortOrder();
    }

    /**
     * Get jenis seleksi by id
     */
    public Optional<JenisSeleksi> getById(Long id) {
        return jenisSeleksiRepository.findById(id);
    }

    /**
     * Get jenis seleksi by code
     */
    public Optional<JenisSeleksi> getByCode(String code) {
        return jenisSeleksiRepository.findByCode(code);
    }

    /**
     * Create new jenis seleksi
     */
    public JenisSeleksi create(JenisSeleksi jenisSeleksi) {
        if (jenisSeleksiRepository.existsByCode(jenisSeleksi.getCode())) {
            throw new IllegalArgumentException("Code sudah digunakan: " + jenisSeleksi.getCode());
        }
        
        jenisSeleksi.setCreatedAt(LocalDateTime.now());
        jenisSeleksi.setUpdatedAt(LocalDateTime.now());
        
        if (jenisSeleksi.getSortOrder() == null) {
            jenisSeleksi.setSortOrder(jenisSeleksiRepository.findAll().size());
        }
        
        if (jenisSeleksi.getIsActive() == null) {
            jenisSeleksi.setIsActive(true);
        }
        
        return jenisSeleksiRepository.save(jenisSeleksi);
    }

    /**
     * Update existing jenis seleksi
     */
    public JenisSeleksi update(Long id, JenisSeleksi updates) {
        JenisSeleksi existing = jenisSeleksiRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jenis Seleksi tidak ditemukan: " + id));
        
        // Check if code is being changed and new code already exists
        if (!existing.getCode().equals(updates.getCode()) && 
            jenisSeleksiRepository.existsByCode(updates.getCode())) {
            throw new IllegalArgumentException("Code sudah digunakan: " + updates.getCode());
        }
        
        existing.setCode(updates.getCode());
        existing.setNama(updates.getNama());
        existing.setDeskripsi(updates.getDeskripsi());
        existing.setFasilitas(updates.getFasilitas());
        existing.setLogoUrl(updates.getLogoUrl());
        existing.setHarga(updates.getHarga());
        existing.setIsActive(updates.getIsActive());
        existing.setSortOrder(updates.getSortOrder());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return jenisSeleksiRepository.save(existing);
    }

    /**
     * Delete jenis seleksi
     */
    public void delete(Long id) {
        if (!jenisSeleksiRepository.existsById(id)) {
            throw new IllegalArgumentException("Jenis Seleksi tidak ditemukan: " + id);
        }
        
        // ✅ FIXED: Delete ALL child records first before deleting parent
        // Delete from PERIOD_JENIS_SELEKSI
        periodJenisSeleksiRepository.deleteByJenisSeleksi_Id(id);
        
        // Delete from SELECTION_PROGRAM_STUDI
        selectionProgramStudiRepository.deleteByJenisSeleksi_Id(id);
        
        entityManager.flush(); // ✅ FIX: Flush to ensure children are deleted before parent
        
        jenisSeleksiRepository.deleteById(id);
    }

    /**
     * Activate/Deactivate jenis seleksi
     */
    public JenisSeleksi toggleActive(Long id) {
        JenisSeleksi js = jenisSeleksiRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Jenis Seleksi tidak ditemukan: " + id));
        js.setIsActive(!js.getIsActive());
        js.setUpdatedAt(LocalDateTime.now());
        return jenisSeleksiRepository.save(js);
    }
}
