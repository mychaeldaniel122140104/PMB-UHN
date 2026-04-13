package com.uhn.pmb.repository;

import com.uhn.pmb.entity.JenisSeleksi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JenisSeleksiRepository extends JpaRepository<JenisSeleksi, Long> {
    Optional<JenisSeleksi> findByCode(String code);
    List<JenisSeleksi> findByIsActiveTrueOrderBySortOrder();
    List<JenisSeleksi> findAllByOrderBySortOrder();
    boolean existsByCode(String code);
}
